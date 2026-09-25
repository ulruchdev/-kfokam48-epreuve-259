import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ClipboardCheck, GraduationCap, KeyRound, Link2, MessagesSquare, Presentation } from 'lucide-react'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ErrorMessage } from '@/components/ErrorMessage'
import { Skeleton } from '@/components/ui/skeleton'
import type { Role } from '@/stores/identityStore'
import { useIdentityStore } from '@/stores/identityStore'
import { usePromotions, useStudents } from './queries'

const roles: {
  value: Role
  label: string
  route: string
  icon: typeof Presentation
  pitch: string
  needsStudent: boolean
}[] = [
  {
    value: 'FORMATEUR',
    label: 'Formateur',
    route: '/formateur',
    icon: Presentation,
    pitch: "Ouvrez la séance, suivez les présences, le tableau de la promotion.",
    needsStudent: false,
  },
  {
    value: 'ETUDIANT',
    label: 'Étudiant',
    route: '/etudiant',
    icon: GraduationCap,
    pitch: 'Marquez votre présence, déposez votre exercice, consultez vos notes.',
    needsStudent: true,
  },
  {
    value: 'RELECTEUR',
    label: 'Relecteur',
    route: '/relecteur',
    icon: ClipboardCheck,
    pitch: "Corrigez l'exercice qui vous a été assigné : une note, un commentaire.",
    needsStudent: true,
  },
]

const steps = [
  {
    n: '1',
    title: 'Code de présence',
    body: "Le formateur ouvre la séance et écrit le code au tableau. Chaque étudiant le saisit pour marquer sa présence.",
    icon: KeyRound,
  },
  {
    n: '2',
    title: "Dépôt de l'exercice",
    body: 'Une fois présent, chaque étudiant dépose le lien de son exercice — remplaçable tant que personne ne le relit.',
    icon: Link2,
  },
  {
    n: '3',
    title: 'Relecture par les pairs',
    body: 'Le système tire un relecteur parmi les présents. Il rend une note sur 20 et un commentaire, sans être nommé.',
    icon: MessagesSquare,
  },
]

/** EF10 (Q1: no password): role first, then promotion, then name if the role needs one. */
export function LandingPage() {
  const navigate = useNavigate()
  const setIdentity = useIdentityStore((state) => state.setIdentity)

  const [role, setRole] = useState<Role | undefined>(undefined)
  const [promotionId, setPromotionId] = useState<number | undefined>(undefined)
  const [etudiantId, setEtudiantId] = useState<number | undefined>(undefined)

  const promotions = usePromotions()
  const students = useStudents(promotionId)

  const selectedRoleOption = roles.find((r) => r.value === role)
  const selectedPromotion = promotions.data?.find((p) => p.id === promotionId)
  const selectedStudent = students.data?.find((s) => s.id === etudiantId)

  const canEnter = Boolean(
    selectedRoleOption &&
      selectedPromotion &&
      (!selectedRoleOption.needsStudent || selectedStudent),
  )

  function handlePickRole(next: Role) {
    setRole(next)
    setPromotionId(undefined)
    setEtudiantId(undefined)
  }

  function handlePromotionChange(value: string) {
    setPromotionId(Number(value))
    setEtudiantId(undefined)
  }

  function handleEnter() {
    if (!selectedRoleOption || !selectedPromotion) return
    if (selectedRoleOption.needsStudent && !selectedStudent) return

    setIdentity({
      role: selectedRoleOption.value,
      promotionId: selectedPromotion.id,
      promotionNom: selectedPromotion.nom,
      etudiantId: selectedStudent?.id,
      etudiantNom: selectedStudent?.nom,
    })
    toast.success(`Bienvenue, ${selectedStudent?.nom ?? selectedRoleOption.label}.`)
    navigate(selectedRoleOption.route)
  }

  return (
    <div className="flex flex-col gap-16 pb-16">
      {/* Hero */}
      <section className="flex flex-col gap-4 pt-6 text-left">
        <h1 className="text-4xl font-semibold tracking-tight text-balance sm:text-5xl">
          Un code au tableau, une présence enregistrée.
        </h1>
        <p className="max-w-[60ch] text-base text-muted-foreground sm:text-lg">
          KFOKAM48 tient le registre de la promotion : présence par code, dépôt
          d'exercice, relecture entre pairs — et un tableau que le formateur
          consulte d'un coup d'œil.
        </p>
      </section>

      {/* 3-step flow */}
      <section aria-labelledby="steps-heading" className="flex flex-col gap-6">
        <h2 id="steps-heading" className="text-xl font-semibold">
          Comment ça marche
        </h2>
        <ol className="grid gap-4 sm:grid-cols-3">
          {steps.map((step) => (
            <li key={step.n}>
              <Card className="h-full gap-3">
                <CardHeader>
                  <div className="flex items-center gap-2 text-ochre">
                    <step.icon className="size-4" aria-hidden="true" />
                    <span className="font-mono text-xs">Étape {step.n}</span>
                  </div>
                  <CardTitle>{step.title}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-muted-foreground">{step.body}</p>
                </CardContent>
              </Card>
            </li>
          ))}
        </ol>
      </section>

      {/* Role picker */}
      <section aria-labelledby="roles-heading" className="flex flex-col gap-6">
        <h2 id="roles-heading" className="text-xl font-semibold">
          Rejoindre
        </h2>
        <div className="grid gap-4 sm:grid-cols-3">
          {roles.map((option) => (
            <Card
              key={option.value}
              className={
                role === option.value ? 'border-2 border-accent shadow-md' : 'border-border'
              }
            >
              <CardHeader>
                <option.icon className="size-6 text-accent" aria-hidden="true" />
                <CardTitle>{option.label}</CardTitle>
                <CardDescription>{option.pitch}</CardDescription>
              </CardHeader>
              <CardContent>
                <Button
                  variant={role === option.value ? 'default' : 'outline'}
                  className="w-full"
                  onClick={() => handlePickRole(option.value)}
                >
                  {option.label}
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>

        {selectedRoleOption && (
          <Card className="max-w-md">
            <CardHeader>
              <CardTitle>Rejoindre en tant que {selectedRoleOption.label}</CardTitle>
              <CardDescription>Aucun mot de passe : choisissez votre nom dans la liste.</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="promotion-select">Promotion</Label>
                {promotions.isLoading && <Skeleton className="h-9 w-full" />}
                {promotions.isError && <ErrorMessage error={promotions.error} />}
                {promotions.data && (
                  <Select value={promotionId ? String(promotionId) : undefined} onValueChange={handlePromotionChange}>
                    <SelectTrigger id="promotion-select" aria-label="Promotion">
                      <SelectValue placeholder="Sélectionner une promotion" />
                    </SelectTrigger>
                    <SelectContent>
                      {promotions.data.map((promotion) => (
                        <SelectItem key={promotion.id} value={String(promotion.id)}>
                          {promotion.nom}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              </div>

              {selectedRoleOption.needsStudent && promotionId !== undefined && (
                <div className="flex flex-col gap-1.5">
                  <span id="etudiant-label" className="font-mono text-xs uppercase text-ochre">
                    Étudiant
                  </span>
                  {students.isLoading && <Skeleton className="h-24 w-full" />}
                  {students.isError && <ErrorMessage error={students.error} />}
                  {students.data && (
                    <RadioGroup
                      aria-labelledby="etudiant-label"
                      value={etudiantId ? String(etudiantId) : undefined}
                      onValueChange={(value) => setEtudiantId(Number(value))}
                      className="max-h-56 overflow-y-auto rounded-md border border-border"
                    >
                      {students.data.map((student) => (
                        <label
                          key={student.id}
                          htmlFor={`student-${student.id}`}
                          className="flex cursor-pointer items-center gap-3 border-b border-border px-3 py-2 last:border-b-0 hover:bg-muted/50"
                        >
                          <RadioGroupItem id={`student-${student.id}`} value={String(student.id)} />
                          <span className="text-sm">{student.nom}</span>
                        </label>
                      ))}
                    </RadioGroup>
                  )}
                </div>
              )}

              <Button size="lg" disabled={!canEnter} onClick={handleEnter}>
                Entrer
              </Button>
            </CardContent>
          </Card>
        )}
      </section>
    </div>
  )
}
