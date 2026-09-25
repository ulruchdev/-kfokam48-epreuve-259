import { useState } from 'react'
import { toast } from 'sonner'
import { AlertCircle, KeyRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useIdentityStore } from '../../stores/identityStore'
import {
  useAddManualPresence,
  useCloseSession,
  useOpenSession,
  useSessionDetail,
  useSessions,
  useTableau,
  useTrainerStudents,
} from './queries'

interface OpenedSession {
  id: number
  code: string
  titre: string
}

/** EF1 (code shown big), EF6 (dashboard), EF7 (manual attendance), EF8 (close). */
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

  const recoveredActive = sessions.data?.find((s) => s.statut === 'OUVERTE')
  const activeSessionId = opened?.id ?? recoveredActive?.id
  // GET /api/sessions/{id} now returns the code too: recover it when the
  // trainer reopens a session they already started (no need for our own state).
  const sessionDetail = useSessionDetail(opened ? undefined : recoveredActive?.id)
  const code = opened?.code ?? sessionDetail.data?.code
  const activeTitre = opened?.titre ?? recoveredActive?.titre
  const hasActiveSession = Boolean(activeSessionId) && !closed

  function handleOpenSession() {
    if (!titre.trim()) return
    openSession.mutate(
      { titre, promotionId: identity.promotionId },
      {
        onSuccess: (data) => {
          setOpened({ id: data.id, code: data.code, titre })
          toast.success('Session ouverte, le code est affiché ci-dessous.')
        },
        onError: (error) => toast.error(error.message),
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
          toast.success(`${student?.nom ?? 'Étudiant'} — ajouté par le formateur`)
          setManualStudentId(undefined)
        },
        onError: (error) => toast.error(error.message),
      },
    )
  }

  function handleCloseSession() {
    if (!activeSessionId) return
    closeSession.mutate(activeSessionId, {
      onSuccess: () => {
        setClosed(true)
        toast.success('Clôture enregistrée.')
      },
      onError: (error) => toast.error(error.message),
    })
  }

  return (
    <section className="flex flex-col gap-8 pb-16">
      <div>
        <h1 className="text-2xl font-semibold">Espace formateur</h1>
        <p className="text-sm text-muted-foreground">{identity.promotionNom}</p>
      </div>

      {closed && (
        <Alert>
          <AlertDescription>Session clôturée.</AlertDescription>
        </Alert>
      )}

      {!closed && hasActiveSession && (
        <Card className="bg-slate text-chalk">
          <CardHeader>
            <div className="flex items-center gap-2 text-chalk/70">
              <KeyRound className="size-4" aria-hidden="true" />
              <CardDescription className="text-chalk/70">
                Code de présence — {activeTitre}
              </CardDescription>
            </div>
            {sessionDetail.isLoading && <Skeleton className="h-16 w-48 bg-chalk/20" />}
            {code && (
              <p
                className="font-mono text-5xl font-semibold tracking-[0.12em]"
                data-testid="session-code"
              >
                {code}
              </p>
            )}
          </CardHeader>
          <CardContent className="flex flex-col gap-4 border-t border-chalk/20 pt-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="manual-presence-select" className="text-chalk/70">
                Ajouter une présence
              </Label>
              <div className="flex flex-wrap gap-2">
                <select
                  id="manual-presence-select"
                  className="h-9 rounded-md border border-chalk/30 bg-chalk/10 px-2 text-sm text-chalk"
                  value={manualStudentId ?? ''}
                  onChange={(event) =>
                    setManualStudentId(event.target.value ? Number(event.target.value) : undefined)
                  }
                >
                  <option value="" disabled>
                    Choisir un étudiant
                  </option>
                  {students.data?.map((student) => (
                    <option key={student.id} value={student.id} className="text-foreground">
                      {student.nom}
                    </option>
                  ))}
                </select>
                <Button
                  variant="secondary"
                  onClick={handleAddPresence}
                  disabled={manualStudentId === undefined || addPresence.isPending}
                >
                  Ajouter
                </Button>
              </div>
              {addPresence.isError && (
                <Alert variant="destructive">
                  <AlertCircle className="size-4" />
                  <AlertDescription>{addPresence.error.message}</AlertDescription>
                </Alert>
              )}
            </div>

            <Button
              variant="destructive"
              className="w-fit"
              onClick={handleCloseSession}
              disabled={closeSession.isPending}
            >
              Clôturer la session
            </Button>
            {closeSession.isError && (
              <Alert variant="destructive">
                <AlertCircle className="size-4" />
                <AlertDescription>{closeSession.error.message}</AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>
      )}

      {!closed && !hasActiveSession && (
        <Card className="max-w-md">
          <CardHeader>
            <CardTitle>Ouvrir une session</CardTitle>
            <CardDescription>Le code s'affiche en grand dès l'ouverture.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="session-titre">Titre de la session</Label>
              <Input
                id="session-titre"
                value={titre}
                onChange={(event) => setTitre(event.target.value)}
                placeholder="Ex. Cours React — semaine 3"
              />
            </div>
            <Button
              onClick={handleOpenSession}
              disabled={!titre.trim() || openSession.isPending}
              className="w-fit"
            >
              Ouvrir la session
            </Button>
            {openSession.isError && (
              <Alert variant="destructive">
                <AlertCircle className="size-4" />
                <AlertDescription>{openSession.error.message}</AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>
      )}

      <div className="flex flex-col gap-3">
        <h2 className="text-xl font-semibold">Tableau de la promotion</h2>
        {tableau.isLoading && (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
          </div>
        )}
        {tableau.isError && (
          <Alert variant="destructive">
            <AlertCircle className="size-4" />
            <AlertDescription>{tableau.error.message}</AlertDescription>
          </Alert>
        )}
        {tableau.data && (
          <div className="rounded-lg border border-border">
            <Table data-testid="dashboard-table">
              <TableHeader>
                <TableRow>
                  <TableHead>Étudiant</TableHead>
                  <TableHead>Présences</TableHead>
                  <TableHead>Exercices déposés</TableHead>
                  <TableHead>Moyenne</TableHead>
                  <TableHead>Relectures en attente</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tableau.data.map((row) => (
                  <TableRow key={row.etudiantId} data-testid={`dashboard-row-${row.etudiantId}`}>
                    <TableCell className="font-medium">{row.nom}</TableCell>
                    <TableCell>
                      {row.presences}
                      {Boolean(row.presencesFormateur) && (
                        <Badge variant="secondary" className="ml-2">
                          dont {row.presencesFormateur} ajoutée
                          {row.presencesFormateur !== 1 ? 's' : ''} par le formateur
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell>{row.exercicesDeposes}</TableCell>
                    <TableCell>{row.moyenne ?? '—'}</TableCell>
                    <TableCell>{row.relecturesEnAttente}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
    </section>
  )
}
