import { useState } from 'react'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import { useIdentityStore } from '../../stores/identityStore'
import type { RelectureAssignee } from '../../api/schemas'
import { useAmendReview, useAssignedReviews, useSubmitReview } from './queries'
import styles from './ReviewerHome.module.css'

/** EF13 (list), EF5 (render note + comment), EF11 (amend until closure). */
export function ReviewerHome() {
  const identity = useIdentityStore((state) => state.identity)!
  const reviews = useAssignedReviews(identity.etudiantId)

  return (
    <section className={styles.screen}>
      <h1>Espace relecteur</h1>
      <p>Relectures assignées à {identity.etudiantNom}.</p>

      {reviews.isLoading && <Loading label="Chargement de vos relectures…" />}
      {reviews.isError && <ErrorMessage error={reviews.error} />}
      {reviews.data && reviews.data.length === 0 && (
        <p>Aucune relecture assignée pour le moment.</p>
      )}
      {reviews.data && reviews.data.length > 0 && (
        <ul className={styles.list}>
          {reviews.data.map((review) => (
            <ReviewRow key={review.id} review={review} relecteurId={identity.etudiantId} />
          ))}
        </ul>
      )}
    </section>
  )
}

function isValidNote(value: string): boolean {
  if (value.trim() === '') return false
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 0 && parsed <= 20
}

function ReviewRow({ review, relecteurId }: { review: RelectureAssignee; relecteurId: number }) {
  const submit = useSubmitReview(relecteurId)
  const amend = useAmendReview(relecteurId)

  const [editing, setEditing] = useState(!review.rendue)
  const [note, setNote] = useState(review.note != null ? String(review.note) : '')
  const [commentaire, setCommentaire] = useState(review.commentaire ?? '')

  const mutation = review.rendue ? amend : submit
  const canSave = isValidNote(note) && commentaire.trim().length > 0

  function startEditing() {
    setNote(review.note != null ? String(review.note) : '')
    setCommentaire(review.commentaire ?? '')
    setEditing(true)
  }

  function handleSave() {
    if (!canSave) return
    mutation.mutate(
      { id: review.id, input: { note: Number(note), commentaire: commentaire.trim(), relecteurId } },
      { onSuccess: () => setEditing(false) },
    )
  }

  return (
    <li className={styles.row} data-testid={`review-row-${review.id}`}>
      <p className={styles.author}>{review.auteurNom}</p>
      <p className={styles.link}>{review.exerciceLien}</p>

      {!editing && review.rendue && (
        <div className={styles.grade}>
          <span className={styles.gradeValue}>{review.note}/20</span>
          <p className={styles.comment}>{review.commentaire}</p>
          <button type="button" onClick={startEditing}>
            Modifier
          </button>
        </div>
      )}

      {editing && (
        <div className={styles.form}>
          <label htmlFor={`note-${review.id}`} className={styles.label}>
            Note (0–20)
          </label>
          <input
            id={`note-${review.id}`}
            className={styles.noteInput}
            type="number"
            min={0}
            max={20}
            value={note}
            onChange={(event) => setNote(event.target.value)}
          />
          <label htmlFor={`commentaire-${review.id}`} className={styles.label}>
            Commentaire
          </label>
          <textarea
            id={`commentaire-${review.id}`}
            className={styles.commentInput}
            value={commentaire}
            onChange={(event) => setCommentaire(event.target.value)}
            rows={3}
          />
          <button type="button" onClick={handleSave} disabled={!canSave || mutation.isPending}>
            {review.rendue ? 'Enregistrer les modifications' : 'Envoyer la relecture'}
          </button>
          {mutation.isError && <ErrorMessage error={mutation.error} />}
        </div>
      )}
    </li>
  )
}
