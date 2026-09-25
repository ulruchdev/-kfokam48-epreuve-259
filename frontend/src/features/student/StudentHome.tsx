import { useState } from 'react'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import { useIdentityStore } from '../../stores/identityStore'
import { useMarkPresence, useMyExercises, useReplaceExerciseLink, useSubmitExercise } from './queries'
import styles from './StudentHome.module.css'

const statutLabel: Record<string, string> = {
  EN_ATTENTE_AFFECTATION: 'En attente d’affectation',
  EN_ATTENTE_RELECTURE: 'En attente de relecture',
  RELU: 'Relu',
}

/** EF2, EF3/EF12, EF9 (RG7: never the reviewer's name) — the three ENF1 mobile-critical screens. */
export function StudentHome() {
  const identity = useIdentityStore((state) => state.identity)!

  const markPresence = useMarkPresence()
  const submitExercise = useSubmitExercise(identity.etudiantId)
  const replaceLink = useReplaceExerciseLink(identity.etudiantId)
  const myExercises = useMyExercises(identity.etudiantId)

  const [code, setCode] = useState('')
  const [markedSessionId, setMarkedSessionId] = useState<number | null>(null)
  const [link, setLink] = useState('')
  const [submittedExerciseId, setSubmittedExerciseId] = useState<number | null>(null)
  const [depositFeedback, setDepositFeedback] = useState<string | null>(null)

  function handleMarkPresence() {
    if (!code.trim()) return
    markPresence.mutate(
      { code: code.trim(), etudiantId: identity.etudiantId },
      { onSuccess: (data) => setMarkedSessionId(data.sessionId) },
    )
  }

  function handleDeposit() {
    if (!link.trim() || markedSessionId === null) return
    submitExercise.mutate(
      { sessionId: markedSessionId, etudiantId: identity.etudiantId, lien: link.trim() },
      {
        onSuccess: (data) => {
          setSubmittedExerciseId(data.id)
          setDepositFeedback('Exercice déposé.')
        },
      },
    )
  }

  function handleReplace() {
    if (!link.trim() || submittedExerciseId === null) return
    replaceLink.mutate(
      { id: submittedExerciseId, lien: link.trim() },
      { onSuccess: () => setDepositFeedback('Lien remplacé.') },
    )
  }

  const depositError = submitExercise.error ?? replaceLink.error
  const depositPending = submitExercise.isPending || replaceLink.isPending

  return (
    <section className={styles.screen}>
      <h1>Espace étudiant</h1>

      <div className={styles.block}>
        <h2>Ma présence</h2>
        <label htmlFor="attendance-code" className={styles.label}>
          Code de la séance
        </label>
        <div className={styles.row}>
          <input
            id="attendance-code"
            className={styles.input}
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder="Ex. AB12CD"
            autoComplete="off"
          />
          <button
            type="button"
            onClick={handleMarkPresence}
            disabled={!code.trim() || markPresence.isPending}
          >
            Valider ma présence
          </button>
        </div>
        {markedSessionId !== null && (
          <p className={styles.confirmation} data-testid="attendance-confirmation">
            Présence enregistrée.
          </p>
        )}
        {markPresence.isError && <ErrorMessage error={markPresence.error} />}
      </div>

      {markedSessionId !== null && (
        <div className={styles.block}>
          <h2>Mon exercice</h2>
          <label htmlFor="exercise-link" className={styles.label}>
            Lien de l'exercice
          </label>
          <div className={styles.row}>
            <input
              id="exercise-link"
              className={styles.input}
              value={link}
              onChange={(event) => setLink(event.target.value)}
              placeholder="https://…"
              autoComplete="off"
            />
            <button
              type="button"
              onClick={submittedExerciseId === null ? handleDeposit : handleReplace}
              disabled={!link.trim() || depositPending}
            >
              {submittedExerciseId === null ? 'Déposer mon exercice' : 'Remplacer le lien'}
            </button>
          </div>
          {depositFeedback && (
            <p className={styles.confirmation} data-testid="deposit-confirmation">
              {depositFeedback}
            </p>
          )}
          {depositError && <ErrorMessage error={depositError} />}
        </div>
      )}

      <div className={styles.block}>
        <h2>Mes exercices</h2>
        {myExercises.isLoading && <Loading label="Chargement de vos exercices…" />}
        {myExercises.isError && <ErrorMessage error={myExercises.error} />}
        {myExercises.data && (
          <ul className={styles.list}>
            {myExercises.data.map((exercice) => (
              <li key={exercice.id} className={styles.card}>
                <p className={styles.cardLink}>{exercice.lien}</p>
                <p className={styles.cardStatut}>{statutLabel[exercice.statut]}</p>
                {exercice.relecture ? (
                  <div className={styles.grade}>
                    <span className={styles.gradeValue}>{exercice.relecture.note}/20</span>
                    <p className={styles.gradeComment}>{exercice.relecture.commentaire}</p>
                  </div>
                ) : (
                  <p className={styles.cardStatut}>En attente de relecture</p>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}
