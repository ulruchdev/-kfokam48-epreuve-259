#!/usr/bin/env bash
# End-to-end API check with curl against a running backend (docker compose up).
# Every endpoint of api/contrat.yaml, nominal case and error cases: HTTP status AND error code.
# Usage: scripts/api-smoke.sh [base_url]   (default http://localhost:8080)
set -uo pipefail

BASE="${1:-http://localhost:8080}"
PROMO=1
PASS=0
FAIL=0
BODY_FILE="$(mktemp)"
trap 'rm -f "$BODY_FILE"' EXIT

# call METHOD PATH [JSON] -> sets STATUS, BODY
call() {
  local method="$1" path="$2" data="${3:-}"
  if [ -n "$data" ]; then
    STATUS=$(curl -s -o "$BODY_FILE" -w '%{http_code}' -X "$method" -H 'Content-Type: application/json' -d "$data" "$BASE$path")
  else
    STATUS=$(curl -s -o "$BODY_FILE" -w '%{http_code}' -X "$method" "$BASE$path")
  fi
  BODY=$(cat "$BODY_FILE")
}

# field EXPR -> reads a value from the last JSON body (python expression on `d`)
field() { printf '%s' "$BODY" | python -c "import json,sys; d=json.load(sys.stdin); print($1)"; }

# check LABEL EXPECTED_STATUS [EXPECTED_ERROR_CODE]
check() {
  local label="$1" expected="$2" code="${3:-}" actual_code=""
  [ -n "$code" ] && actual_code=$(field "d.get('code','')" 2>/dev/null)
  if [ "$STATUS" = "$expected" ] && { [ -z "$code" ] || [ "$actual_code" = "$code" ]; }; then
    PASS=$((PASS + 1)); printf '  PASS  %-3s %-22s %s\n' "$STATUS" "$code" "$label"
  else
    FAIL=$((FAIL + 1)); printf '  FAIL  %-3s %-22s %s  (expected %s %s) %s\n' "$STATUS" "$actual_code" "$label" "$expected" "$code" "$BODY"
  fi
}

echo "== Promotions / students (EF10)"
call GET /api/promotions;                          check "list promotions" 200
call GET /api/promotions/$PROMO/etudiants;         check "list students of promotion" 200
call GET /api/promotions/999/etudiants;            check "students of unknown promotion" 404 PROMOTION_INCONNUE

echo "== Sessions (EF1, DEC-2)"
call POST /api/sessions "{\"titre\":\"Smoke $(date +%s)\",\"promotionId\":$PROMO,\"dureeMinutes\":90}"
check "open session" 201
SESSION=$(field "d['id']"); CODE=$(field "d['code']")
call POST /api/sessions "{\"promotionId\":$PROMO}";                check "open without titre" 400 CHAMP_MANQUANT
call POST /api/sessions '{"titre":"x","promotionId":999}';         check "open for unknown promotion" 404 PROMOTION_INCONNUE
call POST /api/sessions '{"titre":';                               check "malformed JSON" 400 CORPS_INVALIDE
call GET "/api/sessions?promotionId=$PROMO";                       check "list sessions" 200
call GET /api/sessions;                                            check "list without promotionId" 400 PARAMETRE_MANQUANT
call GET /api/sessions/$SESSION;                                   check "session detail" 200
call GET /api/sessions/abc;                                        check "session id not a number" 400 PARAMETRE_INVALIDE
call GET /api/sessions/999999;                                     check "unknown session" 404 SESSION_INCONNUE
call PUT /api/sessions/$SESSION '{"finAt":"2000-01-01T00:00:00Z"}'; check "end before opening" 400 FIN_AVANT_OUVERTURE
call DELETE /api/sessions;                                         check "unsupported verb" 405 METHODE_NON_AUTORISEE

echo "== Attendance (EF2, EF7, RG1-RG3)"
call POST /api/presences "{\"code\":\"$CODE\",\"etudiantId\":1}";  check "mark attendance (student 1)" 201
call POST /api/presences "{\"code\":\"$CODE\",\"etudiantId\":1}";  check "mark twice" 409 DEJA_PRESENT
call POST /api/presences '{"code":"ZZZZZZ","etudiantId":3}';       check "unknown code" 400 CODE_INCONNU
call POST /api/presences "{\"code\":\"$CODE\",\"etudiantId\":999999}"; check "unknown student" 404 ETUDIANT_INCONNU
call POST /api/presences '{"code":"INTRO1","etudiantId":3}';       check "code of the closed demo session" 410 CODE_EXPIRE
call POST /api/sessions/$SESSION/presences '{"etudiantId":2}';     check "manual attendance (student 2)" 201
[ "$(field "d['source']")" = "FORMATEUR" ] && echo "        source = FORMATEUR" || { FAIL=$((FAIL + 1)); echo "  FAIL  source is not FORMATEUR"; }
call POST /api/sessions/$SESSION/presences '{"etudiantId":2}';     check "manual attendance twice" 409 DEJA_PRESENT
call POST /api/sessions/999999/presences '{"etudiantId":2}';       check "manual attendance, unknown session" 404 SESSION_INCONNUE

echo "== Exercises (EF3, EF12, RG10, RG11, RG14)"
call POST /api/exercices "{\"sessionId\":$SESSION,\"etudiantId\":1,\"lien\":\"https://github.com/smoke/ex\"}"
check "submit exercise (student 1)" 201
EXERCISE=$(field "d['id']")
call POST /api/exercices "{\"sessionId\":$SESSION,\"etudiantId\":1,\"lien\":\"https://github.com/smoke/ex\"}"; check "submit twice" 409 EXERCICE_DEJA_DEPOSE
call POST /api/exercices "{\"sessionId\":$SESSION,\"etudiantId\":3,\"lien\":\"https://github.com/smoke/ex\"}"; check "submit without attendance" 400 PRESENCE_REQUISE
call POST /api/exercices "{\"sessionId\":$SESSION,\"etudiantId\":2,\"lien\":\"not a link\"}";          check "invalid link" 400 LIEN_INVALIDE
call POST /api/exercices '{"sessionId":999999,"etudiantId":1,"lien":"https://github.com/smoke/ex"}';   check "unknown session" 404 SESSION_INCONNUE
call GET /api/sessions/$SESSION/exercices;                         check "exercises of the session" 200
call PUT /api/exercices/$EXERCISE '{"lien":"https://github.com/smoke/ex-v2"}'; check "replace link before review" 200
call PUT /api/exercices/999999 '{"lien":"https://github.com/smoke/ex-v2"}';   check "replace link, unknown exercise" 404 EXERCICE_INCONNU

echo "== Reviews (EF5, EF11, EF13, RG4, RG8, RG9)"
call GET "/api/relectures?relecteurId=2";                          check "assigned reviews of student 2" 200
REVIEW=$(field "[r['id'] for r in d if r['exerciceId']==$EXERCISE][0]")
call POST /api/relectures/$REVIEW '{"note":21,"commentaire":"x","relecteurId":2}';     check "grade above 20" 400 NOTE_INVALIDE
call POST /api/relectures/$REVIEW '{"note":12.5,"commentaire":"x","relecteurId":2}';   check "non-integer grade" 400 NOTE_INVALIDE
call POST /api/relectures/$REVIEW '{"note":15,"commentaire":"x","relecteurId":1}';     check "author reviews own exercise" 403 AUTO_RELECTURE
call POST /api/relectures/$REVIEW '{"note":15,"commentaire":"x","relecteurId":4}';     check "not the assignee" 403 RELECTURE_NON_ASSIGNEE
call POST /api/relectures/$REVIEW '{"note":15,"commentaire":"Clean layers","relecteurId":2}'; check "render review" 200
call POST /api/relectures/$REVIEW '{"note":16,"commentaire":"again","relecteurId":2}';  check "render twice" 409 RELECTURE_DEJA_RENDUE
call PUT /api/relectures/$REVIEW '{"note":17,"commentaire":"Amended","relecteurId":2}'; check "amend before closure" 200
call POST /api/relectures/999999 '{"note":15,"commentaire":"x"}';                     check "unknown review" 404 RELECTURE_INCONNUE
call PUT /api/exercices/$EXERCISE '{"lien":"https://github.com/smoke/ex-v3"}';         check "replace link after review" 409 RELECTURE_COMMENCEE
call GET "/api/etudiants/1/exercices?sessionId=$SESSION";          check "student 1 sees grade" 200
[ "$(field "d[0]['relecture']['note']")" = "17" ] && [ "$(field "'relecteurId' in str(d)")" = "False" ] \
  && echo "        grade 17 visible, reviewer never exposed (RG7)" || { FAIL=$((FAIL + 1)); echo "  FAIL  RG7 view: $BODY"; }

echo "== Dashboard (EF6, Q16)"
call GET "/api/tableau?promotionId=$PROMO";                        check "dashboard" 200
echo "        $(field "'; '.join(f\"{r['nom']}: presences={r['presences']} (formateur={r['presencesFormateur']}) depots={r['exercicesDeposes']} moyenne={r['moyenne']} a_relire={r['relecturesEnAttente']}\" for r in d)")"
call GET "/api/tableau?promotionId=999";                           check "dashboard, unknown promotion" 404 PROMOTION_INCONNUE
call GET /api/tableau;                                             check "dashboard without promotionId" 400 PARAMETRE_MANQUANT

echo "== Closure (EF8, RG9, RG10)"
call POST /api/sessions/$SESSION/cloture;                          check "close session" 204
call POST /api/sessions/$SESSION/cloture;                          check "close twice" 409 SESSION_DEJA_CLOTUREE
call PUT /api/relectures/$REVIEW '{"note":10,"commentaire":"late","relecteurId":2}'; check "amend after closure" 409 SESSION_CLOTUREE
call POST /api/presences "{\"code\":\"$CODE\",\"etudiantId\":4}";  check "code after closure" 410 CODE_EXPIRE

echo "== Documentation"
call GET /v3/api-docs;                                             check "OpenAPI document" 200
call GET /contrat.yaml;                                            check "frozen contract" 200

echo
echo "RESULT: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
