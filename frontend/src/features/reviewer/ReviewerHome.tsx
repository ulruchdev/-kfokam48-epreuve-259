import { useState } from 'react'
import { toast } from 'sonner'
import { AlertCircle } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { useIdentityStore } from '../../stores/identityStore'
import type { RelectureAssignee } from '../../api/schemas'
import { useAmendReview, useAssignedReviews, useSubmitReview } from './queries'

/** EF13 (list), EF5 (render note + comment), EF11 (amend until closure). */
export function ReviewerHome() {
  const identity = useIdentityStore((state) => state.identity)!
  const relecteurId = identity.etudiantId!
  const reviews = useAssignedReviews(relecteurId)

  return (
    <section className="mx-auto flex w-full max-w-sm flex-col gap-6 pb-16 sm:max-w-xl">
      <div>
        <h1 className="text-2xl font-semibold">Espace relecteur</h1>
        <p className="text-sm text-muted-foreground">Relectures assignées à {identity.etudiantNom}.</p>
      </div>

      {reviews.isLoading && (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      )}
      {reviews.isError && (
        <Alert variant="destructive">
          <AlertCircle className="size-4" />
          <AlertDescription>{reviews.error.message}</AlertDescription>
        </Alert>
      )}
      {reviews.data && reviews.data.length === 0 && (
        <p className="text-sm text-muted-foreground">Aucune relecture assignée pour le moment.</p>
      )}
      {reviews.data && reviews.data.length > 0 && (
        <ul className="flex flex-col gap-3">
          {reviews.data.map((review) => (
            <ReviewRow key={review.id} review={review} relecteurId={relecteurId} />
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
      {
        onSuccess: () => {
          setEditing(false)
          toast.success('Relecture enregistrée.')
        },
        onError: (error) => toast.error(error.message),
      },
    )
  }

  return (
    <li data-testid={`review-row-${review.id}`}>
      <Card className="gap-3 py-4">
        <CardContent className="flex flex-col gap-1.5 px-4">
          <p className="font-medium">{review.auteurNom}</p>
          <p className="break-words font-mono text-xs text-muted-foreground">{review.exerciceLien}</p>

          {!editing && review.rendue && (
            <div className="mt-1 flex flex-col items-start gap-2 border-t border-border pt-3">
              <span className="font-mono text-lg font-semibold text-ochre">{review.note}/20</span>
              <p className="text-sm">{review.commentaire}</p>
              <Button variant="link" className="h-auto p-0" onClick={startEditing}>
                Modifier
              </Button>
            </div>
          )}

          {editing && (
            <div className="mt-1 flex flex-col gap-2 border-t border-border pt-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor={`note-${review.id}`}>Note (0–20)</Label>
                <Input
                  id={`note-${review.id}`}
                  className="w-20"
                  type="number"
                  min={0}
                  max={20}
                  value={note}
                  onChange={(event) => setNote(event.target.value)}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor={`commentaire-${review.id}`}>Commentaire</Label>
                <Textarea
                  id={`commentaire-${review.id}`}
                  value={commentaire}
                  onChange={(event) => setCommentaire(event.target.value)}
                  rows={3}
                />
              </div>
              <Button
                className="w-fit"
                onClick={handleSave}
                disabled={!canSave || mutation.isPending}
              >
                {review.rendue ? 'Enregistrer les modifications' : 'Envoyer la relecture'}
              </Button>
              {mutation.isError && (
                <Alert variant="destructive">
                  <AlertCircle className="size-4" />
                  <AlertDescription>{mutation.error.message}</AlertDescription>
                </Alert>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </li>
  )
}
