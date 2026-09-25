package com.kfokam48.attendance.web.dto;

import com.kfokam48.attendance.web.erreur.MissingFieldException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * All request/response payloads (B3: no JPA entity is ever exposed as JSON).
 * Error messages stay in French: they are imposed by the API contract.
 */
public final class Dto {

    private Dto() {}

    // ---------- requests ----------

    public record OpenSessionRequest(
            @NotBlank(message = "titre") String titre,
            @NotNull(message = "promotionId") Long promotionId,
            Integer durationMinutes) {   // optional, DEC-2 default 120

        public int effectiveDuration() {
            if (durationMinutes == null) return 120;
            if (durationMinutes < 15 || durationMinutes > 480) {
                throw new MissingFieldException("dureeMinutes doit être entre 15 et 480");
            }
            return durationMinutes;
        }
    }

    public record MarkAttendanceRequest(
            @NotBlank(message = "code") String code,
            @NotNull(message = "etudiantId") Long etudiantId) {}

    public record SubmitExerciseRequest(
            @NotNull(message = "sessionId") Long sessionId,
            @NotNull(message = "etudiantId") Long etudiantId,
            @NotBlank(message = "lien") String lien) {}

    public record SubmitReviewRequest(
            @NotNull(message = "note") Integer note,
            @NotNull(message = "commentaire") String commentaire) {}

    public record AddManualAttendanceRequest(
            @NotNull(message = "etudiantId") Long etudiantId) {}

    public record AdjustEndTimeRequest(@NotNull(message = "finAt") OffsetDateTime endTime) {}

    public record ReplaceLinkRequest(@NotBlank(message = "lien") String lien) {}

    // ---------- responses ----------

    /** POST /api/sessions 201 — the 4 imposed fields + additive extensions (finAt, statut). */
    public record SessionOpenedResponse(
            Long id, String code, OffsetDateTime ouvertureAt, OffsetDateTime expirationAt,
            OffsetDateTime finAt, String statut) {}

    /** POST /api/presences 201 — the 4 imposed required fields. */
    public record AttendanceResponse(
            Long id, Long sessionId, Long etudiantId, String source) {}

    /** POST /api/exercices 201 — the 2 imposed required fields. */
    public record ExerciseSubmittedResponse(Long id, String statut) {}

    /** GET /api/promotions. */
    public record PromotionResponse(Long id, String nom) {}

    /** GET /api/promotions/{id}/etudiants. */
    public record StudentResponse(Long id, String nom) {}

    /** GET /api/sessions/{id} and PUT /api/sessions/{id}. */
    public record SessionDetailResponse(
            Long id, Long promotionId, String titre, String code,
            OffsetDateTime ouvertureAt, OffsetDateTime expirationAt,
            OffsetDateTime finAt, String statut) {}

    /** GET /api/sessions?promotionId=. */
    public record SessionSummaryResponse(
            Long id, String titre, OffsetDateTime ouvertureAt, OffsetDateTime expirationAt,
            OffsetDateTime finAt, String statut) {}

    /** GET /api/sessions/{id}/exercices (trainer view). */
    public record ExerciseResponse(
            Long id, Long sessionId, Long etudiantId, String etudiantNom,
            String lien, String statut) {}

    /** GET /api/etudiants/{id}/exercices — author view, NEVER the reviewer name (RG7). */
    public record ExerciseWithReviewResponse(
            Long id, Long sessionId, String lien, String statut, ReceivedReviewResponse review) {}

    public record ReceivedReviewResponse(Integer note, String commentaire) {}

    /** GET /api/relectures?relecteurId= — reviewer view. */
    public record AssignedReviewResponse(
            Long id, Long exerciceId, String exerciceLien, String auteurNom,
            boolean rendue, Integer note, String commentaire) {}

    /** GET /api/tableau?promotionId= — the 6 imposed required fields per student. */
    public record DashboardRowResponse(
            Long etudiantId, String nom, int presences, int exercicesDeposes,
            Double moyenne, int relecturesEnAttente) {}

    /** PUT /api/relectures/{id} 200 (amendment). */
    public record ReviewResponse(
            Long id, Long exerciceId, Long relecteurId, Integer note,
            String commentaire, boolean rendue) {}
}
