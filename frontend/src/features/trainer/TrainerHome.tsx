import { useState } from 'react'
import { toast } from 'sonner'
import { AlertCircle, KeyRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useIdentityStore } from '../../stores/identityStore'
import { CodeCountdown } from './CodeCountdown'
import {
  useAddManualPresence,
  useAdjustSessionEnd,
  useCloseSession,
  useOpenSession,
  useSessionDetail,
  useSessions,
  useTableau,
  useTrainerStudents,
} from './queries'

const statutLabel: Record<string, string> = {
  OUVERTE: 'Ouverte',
  TERMINEE: 'Terminée',
  CLOTUREE: 'Clôturée',
}

interface KnownSession {
  id: number
  code: string
  titre: string
  expirationAt: string
  finAt?: string
}

/** EF1/DEC-2 (duration, countdown), EF6 (dashboard), EF7 (manual attendance), EF8 (close), US-40 (list, reopen, adjust end). */
export function TrainerHome() {
  const identity = useIdentityStore((state) => state.identity)!

  const sessions = useSessions(identity.promotionId)
  const tableau = useTableau(identity.promotionId)
  const students = useTrainerStudents(identity.promotionId)
  const openSession = useOpenSession()
  const addPresence = useAddManualPresence(identity.promotionId)
  const closeSession = useCloseSession(identity.promotionId)
  const adjustEnd = useAdjustSessionEnd(identity.promotionId)

  const [titre, setTitre] = useState('')
  const [duree, setDuree] = useState(120)
  const [knownSession, setKnownSession] = useState<KnownSession | null>(null)
  const [selectedSessionId, setSelectedSessionId] = useState<number | undefined>(undefined)
  const [justClosed, setJustClosed] = useState(false)
  const [manualStudentId, setManualStudentId] = useState<number | undefined>(undefined)
  const [newFinAt, setNewFinAt] = useState('')
  const [closeDialogOpen, setCloseDialogOpen] = useState(false)

  // Known locally right after opening: skips a round-trip. Otherwise fetch
  // GET /api/sessions/{id}, which now returns the code too (US-40).
  const sessionDetail = useSessionDetail(knownSession ? undefined : selectedSessionId)
  const activeId = knownSession?.id ?? selectedSessionId
  const code = knownSession?.code ?? sessionDetail.data?.code
  const activeTitre = knownSession?.titre ?? sessionDetail.data?.titre
  const expirationAt = knownSession?.expirationAt ?? sessionDetail.data?.expirationAt
  const hasActiveSession = Boolean(activeId) && !justClosed

  function handleOpenSession() {
    if (!titre.trim()) return
    openSession.mutate(
      { titre, promotionId: identity.promotionId, dureeMinutes: duree },
      {
        onSuccess: (data) => {
          setKnownSession({ id: data.id, code: data.code, titre, expirationAt: data.expirationAt, finAt: data.finAt })
          setSelectedSessionId(data.id)
          setJustClosed(false)
          toast.success('Session ouverte, le code est affiché ci-dessous.')
        },
        onError: (error) => toast.error(error.message),
      },
    )
  }

  function handleReopen(id: number) {
    setKnownSession(null)
    setSelectedSessionId(id)
    setJustClosed(false)
  }

  function handleAddPresence() {
    if (manualStudentId === undefined || !activeId) return
    const student = students.data?.find((s) => s.id === manualStudentId)
    addPresence.mutate(
      { sessionId: activeId, etudiantId: manualStudentId },
      {
        onSuccess: () => {
          toast.success(`${student?.nom ?? 'Étudiant'} — ajouté par le formateur`)
          setManualStudentId(undefined)
        },
        onError: (error) => toast.error(error.message),
      },
    )
  }

  function handleAdjustEnd() {
    if (!activeId || !newFinAt) return
    adjustEnd.mutate(
      { id: activeId, finAt: new Date(newFinAt).toISOString() },
      {
        onSuccess: () => {
          toast.success('Heure de fin mise à jour.')
          setNewFinAt('')
        },
        onError: (error) => toast.error(error.message),
      },
    )
  }

  function handleConfirmClose() {
    if (!activeId) return
    closeSession.mutate(activeId, {
      onSuccess: () => {
        setJustClosed(true)
        setCloseDialogOpen(false)
        toast.success('Clôture enregistrée.')
      },
      onError: (error) => {
        setCloseDialogOpen(false)
        toast.error(error.message)
      },
    })
  }

  return (
    <section className="flex flex-col gap-8 pb-16">
      <div>
        <h1 className="text-2xl font-semibold">Espace formateur</h1>
        <p className="text-sm text-muted-foreground">{identity.promotionNom}</p>
      </div>

      {justClosed && (
        <Alert>
          <AlertDescription>Session clôturée.</AlertDescription>
        </Alert>
      )}

      {!justClosed && hasActiveSession && (
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
            {expirationAt && <CodeCountdown key={expirationAt} expirationAt={expirationAt} />}
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

            <div className="flex flex-col gap-1.5 border-t border-chalk/20 pt-4">
              <Label htmlFor="session-fin" className="text-chalk/70">
                Nouvelle heure de fin
              </Label>
              <div className="flex flex-wrap gap-2">
                <Input
                  id="session-fin"
                  type="datetime-local"
                  className="w-auto border-chalk/30 bg-chalk/10 text-chalk"
                  value={newFinAt}
                  onChange={(event) => setNewFinAt(event.target.value)}
                />
                <Button
                  variant="secondary"
                  onClick={handleAdjustEnd}
                  disabled={!newFinAt || adjustEnd.isPending}
                >
                  Mettre à jour la fin
                </Button>
              </div>
              {adjustEnd.isError && (
                <Alert variant="destructive">
                  <AlertCircle className="size-4" />
                  <AlertDescription>{adjustEnd.error.message}</AlertDescription>
                </Alert>
              )}
            </div>

            <Button
              variant="destructive"
              className="w-fit"
              onClick={() => setCloseDialogOpen(true)}
              disabled={closeSession.isPending}
            >
              Clôturer la session
            </Button>
          </CardContent>
        </Card>
      )}

      <Dialog open={closeDialogOpen} onOpenChange={setCloseDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Clôturer la session ?</DialogTitle>
            <DialogDescription>
              Après clôture, plus aucun dépôt ni correction de relecture ne sera possible
              (RG9, RG10). Cette action est définitive.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCloseDialogOpen(false)}>
              Annuler
            </Button>
            <Button variant="destructive" onClick={handleConfirmClose} disabled={closeSession.isPending}>
              Confirmer la clôture
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {!hasActiveSession && !justClosed && (
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
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="session-duree">Durée (minutes)</Label>
              <Input
                id="session-duree"
                type="number"
                min={15}
                max={480}
                step={15}
                value={duree}
                onChange={(event) => setDuree(Number(event.target.value))}
                className="w-28"
              />
            </div>
            <Button
              onClick={handleOpenSession}
              disabled={!titre.trim() || duree < 15 || duree > 480 || openSession.isPending}
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
        <h2 className="text-xl font-semibold">Sessions de la promotion</h2>
        {sessions.isLoading && <Skeleton className="h-8 w-full" />}
        {sessions.isError && (
          <Alert variant="destructive">
            <AlertCircle className="size-4" />
            <AlertDescription>{sessions.error.message}</AlertDescription>
          </Alert>
        )}
        {sessions.data && sessions.data.length === 0 && (
          <p className="text-sm text-muted-foreground">Aucune session pour le moment.</p>
        )}
        {sessions.data && sessions.data.length > 0 && (
          <div className="rounded-lg border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Titre</TableHead>
                  <TableHead>Statut</TableHead>
                  <TableHead>Ouverture</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {sessions.data.map((s) => (
                  <TableRow key={s.id} data-testid={`session-row-${s.id}`}>
                    <TableCell className="font-medium">{s.titre}</TableCell>
                    <TableCell>
                      <Badge variant={s.statut === 'OUVERTE' ? 'success' : 'outline'}>
                        {statutLabel[s.statut] ?? s.statut}
                      </Badge>
                    </TableCell>
                    <TableCell>{new Date(s.ouvertureAt).toLocaleString('fr-FR')}</TableCell>
                    <TableCell>
                      <Button variant="link" className="h-auto p-0" onClick={() => handleReopen(s.id)}>
                        Reprendre
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

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
