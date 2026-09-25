import { useState } from 'react'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import { useIdentityStore } from '../../stores/identityStore'
import {
  useAddManualPresence,
  useCloseSession,
  useOpenSession,
  useSessions,
  useTableau,
  useTrainerStudents,
} from './queries'
import styles from './TrainerHome.module.css'

interface OpenedSession {
  id: number
  code: string
  titre: string
}

export function TrainerHome() {
  const identity = useIdentityStore((state) => state.identity)!

  const sessions = useSessions(identity.promotionId)
  const tableau = useTableau(identity.promotionId)
  const students = useTrainerStudents(identity.promotionId)
  const openSession = useOpenSession()
  const addPresence = useAddManualPresence(identity.promotionId)
  const closeSession = useCloseSession(identity.promotionId)

  const [titre, setTitre] = useState('')
  const [opened, setOpened] = useState<OpenedSession | null>(null)
  const [closed, setClosed] = useState(false)
  const [manualStudentId, setManualStudentId] = useState<number | undefined>(undefined)
  const [manualFeedback, setManualFeedback] = useState<string | null>(null)

  const recoveredActive = sessions.data?.find((s) => s.statut === 'OUVERTE')
  const activeSessionId = opened?.id ?? recoveredActive?.id
  const activeTitre = opened?.titre ?? recoveredActive?.titre
  const hasActiveSession = Boolean(activeSessionId) && !closed

  function handleOpenSession() {
    if (!titre.trim()) return
    openSession.mutate(
      { titre, promotionId: identity.promotionId },
      {
        onSuccess: (data) => {
          setOpened({ id: data.id, code: data.code, titre })
        },
      },
    )
  }

  function handleAddPresence() {
    if (manualStudentId === undefined || !activeSessionId) return
    const student = students.data?.find((s) => s.id === manualStudentId)
    addPresence.mutate(
      { sessionId: activeSessionId, etudiantId: manualStudentId },
      {
        onSuccess: () => {
          setManualFeedback(`${student?.nom ?? 'Étudiant'} — ajouté par le formateur`)
          setManualStudentId(undefined)
        },
      },
    )
  }

  function handleCloseSession() {
    if (!activeSessionId) return
    closeSession.mutate(activeSessionId, {
      onSuccess: () => setClosed(true),
    })
  }

  return (
    <section className={styles.screen}>
      <h1>Espace formateur — {identity.promotionNom}</h1>

      {closed && <p className={styles.closedNotice}>Session clôturée.</p>}

      {!closed && hasActiveSession && (
        <div className={styles.sessionPanel}>
          {opened ? (
            <>
              <p className={styles.sessionLabel}>Code de présence — {opened.titre}</p>
              <p className={styles.code} data-testid="session-code">
                {opened.code}
              </p>
            </>
          ) : (
            <p className={styles.sessionLabel}>
              Session déjà ouverte : {activeTitre} (code déjà communiqué)
            </p>
          )}

          <div className={styles.manualBlock}>
            <label htmlFor="manual-presence-select" className={styles.label}>
              Ajouter une présence
            </label>
            <div className={styles.manualRow}>
              <select
                id="manual-presence-select"
                className={styles.select}
                value={manualStudentId ?? ''}
                onChange={(event) =>
                  setManualStudentId(event.target.value ? Number(event.target.value) : undefined)
                }
              >
                <option value="" disabled>
                  Choisir un étudiant
                </option>
                {students.data?.map((student) => (
                  <option key={student.id} value={student.id}>
                    {student.nom}
                  </option>
                ))}
              </select>
              <button
                type="button"
                onClick={handleAddPresence}
                disabled={manualStudentId === undefined || addPresence.isPending}
              >
                Ajouter
              </button>
            </div>
            {manualFeedback && <p className={styles.feedback}>{manualFeedback}</p>}
            {addPresence.isError && <ErrorMessage error={addPresence.error} />}
          </div>

          <button
            type="button"
            className={styles.closeButton}
            onClick={handleCloseSession}
            disabled={closeSession.isPending}
          >
            Clôturer la session
          </button>
          {closeSession.isError && <ErrorMessage error={closeSession.error} />}
        </div>
      )}

      {!closed && !hasActiveSession && (
        <div className={styles.openPanel}>
          <label htmlFor="session-titre" className={styles.label}>
            Titre de la session
          </label>
          <input
            id="session-titre"
            className={styles.input}
            value={titre}
            onChange={(event) => setTitre(event.target.value)}
            placeholder="Ex. Cours React — semaine 3"
          />
          <button
            type="button"
            className={styles.openButton}
            onClick={handleOpenSession}
            disabled={!titre.trim() || openSession.isPending}
          >
            Ouvrir la session
          </button>
          {openSession.isError && <ErrorMessage error={openSession.error} />}
        </div>
      )}

      <h2>Tableau de la promotion</h2>
      {tableau.isLoading && <Loading label="Chargement du tableau…" />}
      {tableau.isError && <ErrorMessage error={tableau.error} />}
      {tableau.data && (
        <table className={styles.table} data-testid="dashboard-table">
          <thead>
            <tr>
              <th>Étudiant</th>
              <th>Présences</th>
              <th>Exercices déposés</th>
              <th>Moyenne</th>
              <th>Relectures en attente</th>
            </tr>
          </thead>
          <tbody>
            {tableau.data.map((row) => (
              <tr key={row.etudiantId} data-testid={`dashboard-row-${row.etudiantId}`}>
                <td>{row.nom}</td>
                <td>{row.presences}</td>
                <td>{row.exercicesDeposes}</td>
                <td>{row.moyenne ?? '—'}</td>
                <td>{row.relecturesEnAttente}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
