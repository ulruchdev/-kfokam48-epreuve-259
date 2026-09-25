import { useState } from 'react'
import { toast } from 'sonner'
import { AlertCircle } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { useIdentityStore } from '../../stores/identityStore'
import { useMarkPresence, useMyExercises, useReplaceExerciseLink, useSubmitExercise } from './queries'

const statutLabel: Record<string, string> = {
  EN_ATTENTE_AFFECTATION: 'En attente d’affectation',
  EN_ATTENTE_RELECTURE: 'En attente de relecture',
  RELU: 'Relu',
}

/** EF2, EF3/EF12, EF9 (RG7: never the reviewer's name) — the three ENF1 mobile-critical screens. */
export function StudentHome() {
  const identity = useIdentityStore((state) => state.identity)!
  const etudiantId = identity.etudiantId!

  const markPresence = useMarkPresence()
  const submitExercise = useSubmitExercise(etudiantId)
  const replaceLink = useReplaceExerciseLink(etudiantId)
  const myExercises = useMyExercises(etudiantId)

  const [code, setCode] = useState('')
  const [markedSessionId, setMarkedSessionId] = useState<number | null>(null)
  const [link, setLink] = useState('')
  const [submittedExerciseId, setSubmittedExerciseId] = useState<number | null>(null)
  const [depositFeedback, setDepositFeedback] = useState<string | null>(null)

  function handleMarkPresence() {
    if (!code.trim()) return
    markPresence.mutate(
      { code: code.trim(), etudiantId },
      {
        onSuccess: (data) => {
          setMarkedSessionId(data.sessionId)
          toast.success('Présence enregistrée.')
        },
        onError: (error) => toast.error(error.message),
      },
    )
  }

  function handleDeposit() {
    if (!link.trim() || markedSessionId === null) return
    submitExercise.mutate(
      { sessionId: markedSessionId, etudiantId, lien: link.trim() },
      {
        onSuccess: (data) => {
          setSubmittedExerciseId(data.id)
          setDepositFeedback('Exercice déposé.')
          toast.success('Exercice déposé.')
        },
        onError: (error) => toast.error(error.message),
      },
    )
  }

  function handleReplace() {
    if (!link.trim() || submittedExerciseId === null) return
    replaceLink.mutate(
      { id: submittedExerciseId, lien: link.trim() },
      {
        onSuccess: () => {
          setDepositFeedback('Lien remplacé.')
          toast.success('Lien remplacé.')
        },
        onError: (error) => toast.error(error.message),
      },
    )
  }

  const depositError = submitExercise.error ?? replaceLink.error
  const depositPending = submitExercise.isPending || replaceLink.isPending

  return (
    <section className="mx-auto flex w-full max-w-sm flex-col gap-6 pb-16 sm:max-w-xl">
      <h1 className="text-2xl font-semibold">Espace étudiant</h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Ma présence</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          <Label htmlFor="attendance-code">Code de la séance</Label>
          <div className="flex flex-wrap gap-2">
            <Input
              id="attendance-code"
              className="flex-1 min-w-0"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              placeholder="Ex. AB12CD"
              autoComplete="off"
            />
            <Button onClick={handleMarkPresence} disabled={!code.trim() || markPresence.isPending}>
              Valider ma présence
            </Button>
          </div>
          {markedSessionId !== null && (
            <p className="text-sm font-medium text-pine" data-testid="attendance-confirmation">
              Présence enregistrée.
            </p>
          )}
          {markPresence.isError && (
            <Alert variant="destructive">
              <AlertCircle className="size-4" />
              <AlertDescription>{markPresence.error.message}</AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      {markedSessionId !== null && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Mon exercice</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <Label htmlFor="exercise-link">Lien de l'exercice</Label>
            <div className="flex flex-wrap gap-2">
              <Input
                id="exercise-link"
                className="flex-1 min-w-0"
                value={link}
                onChange={(event) => setLink(event.target.value)}
                placeholder="https://…"
                autoComplete="off"
              />
              <Button
                variant="secondary"
                onClick={submittedExerciseId === null ? handleDeposit : handleReplace}
                disabled={!link.trim() || depositPending}
              >
                {submittedExerciseId === null ? 'Déposer mon exercice' : 'Remplacer le lien'}
              </Button>
            </div>
            {depositFeedback && (
              <p className="text-sm font-medium text-pine" data-testid="deposit-confirmation">
                {depositFeedback}
              </p>
            )}
            {depositError && (
              <Alert variant="destructive">
                <AlertCircle className="size-4" />
                <AlertDescription>{depositError.message}</AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>
      )}

      <div className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Mes exercices</h2>
        {myExercises.isLoading && (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        )}
        {myExercises.isError && (
          <Alert variant="destructive">
            <AlertCircle className="size-4" />
            <AlertDescription>{myExercises.error.message}</AlertDescription>
          </Alert>
        )}
        {myExercises.data && (
          <ul className="flex flex-col gap-3">
            {myExercises.data.map((exercice) => (
              <li key={exercice.id}>
                <Card className="gap-2 py-4">
                  <CardContent className="flex flex-col gap-1.5 px-4">
                    <p className="break-words font-mono text-xs text-muted-foreground">
                      {exercice.lien}
                    </p>
                    <Badge variant="outline" className="w-fit">
                      {statutLabel[exercice.statut]}
                    </Badge>
                    {exercice.relecture ? (
                      <div className="mt-1 border-t border-border pt-2">
                        <span className="font-mono font-semibold text-ochre">
                          {exercice.relecture.note}/20
                        </span>
                        <p className="mt-1 text-sm">{exercice.relecture.commentaire}</p>
                      </div>
                    ) : (
                      <p className="text-sm text-muted-foreground">En attente de relecture</p>
                    )}
                  </CardContent>
                </Card>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}
