import { z } from 'zod'

/** Zod mirrors of api/contrat.yaml — every API response is parsed at the boundary (F3). */

export const promotionSchema = z.object({
  id: z.number(),
  nom: z.string(),
})
export type Promotion = z.infer<typeof promotionSchema>

export const etudiantSchema = z.object({
  id: z.number(),
  nom: z.string(),
})
export type Etudiant = z.infer<typeof etudiantSchema>

export const sessionStatutSchema = z.enum(['OUVERTE', 'TERMINEE', 'CLOTUREE'])
export type SessionStatut = z.infer<typeof sessionStatutSchema>

export const sessionOuvertureSchema = z.object({
  id: z.number(),
  code: z.string(),
  ouvertureAt: z.string(),
  expirationAt: z.string(),
  finAt: z.string().optional(),
  statut: sessionStatutSchema.optional(),
})
export type SessionOuverture = z.infer<typeof sessionOuvertureSchema>

export const sessionResumeSchema = z.object({
  id: z.number(),
  titre: z.string(),
  ouvertureAt: z.string(),
  expirationAt: z.string(),
  finAt: z.string(),
  statut: sessionStatutSchema,
})
export type SessionResume = z.infer<typeof sessionResumeSchema>

export const sessionDetailSchema = sessionResumeSchema.extend({
  promotionId: z.number(),
  /** GET/PUT /api/sessions/{id} now return the code too (used to recover a reopened session). */
  code: z.string(),
})
export type SessionDetail = z.infer<typeof sessionDetailSchema>

export const presenceSourceSchema = z.enum(['ETUDIANT', 'FORMATEUR'])

export const presenceSchema = z.object({
  id: z.number(),
  sessionId: z.number(),
  etudiantId: z.number(),
  source: presenceSourceSchema,
})
export type Presence = z.infer<typeof presenceSchema>

export const exerciceStatutSchema = z.enum([
  'EN_ATTENTE_AFFECTATION',
  'EN_ATTENTE_RELECTURE',
  'RELU',
])
export type ExerciceStatut = z.infer<typeof exerciceStatutSchema>

export const exerciceDeposeSchema = z.object({
  id: z.number(),
  statut: exerciceStatutSchema,
})
export type ExerciceDepose = z.infer<typeof exerciceDeposeSchema>

export const exerciceSchema = z.object({
  id: z.number(),
  sessionId: z.number(),
  etudiantId: z.number(),
  etudiantNom: z.string(),
  lien: z.string(),
  statut: exerciceStatutSchema,
})
export type Exercice = z.infer<typeof exerciceSchema>

/**
 * Contract v1.3 (RG16): retained grade = average of the rendered reviews, provisional while
 * only one of the two is rendered. Never any reviewer identity (RG7): unknown keys are stripped.
 */
const evaluationSchema = z
  .object({
    note: z.number(),
    provisoire: z.boolean(),
    commentaires: z.array(z.string()),
  })
  .nullable()

export const exerciceAvecRelectureSchema = exerciceSchema.extend({
  evaluation: evaluationSchema.optional(),
})
export type ExerciceAvecRelecture = z.infer<typeof exerciceAvecRelectureSchema>

export const relectureSchema = z.object({
  id: z.number(),
  exerciceId: z.number(),
  relecteurId: z.number(),
  note: z.number().nullable(),
  commentaire: z.string().nullable(),
  rendue: z.boolean(),
})
export type Relecture = z.infer<typeof relectureSchema>

export const relectureAssigneeSchema = z.object({
  id: z.number(),
  exerciceId: z.number(),
  exerciceLien: z.string(),
  auteurNom: z.string(),
  rendue: z.boolean(),
  note: z.number().nullable().optional(),
  commentaire: z.string().nullable().optional(),
})
export type RelectureAssignee = z.infer<typeof relectureAssigneeSchema>

export const tableauLigneSchema = z.object({
  etudiantId: z.number(),
  nom: z.string(),
  presences: z.number(),
  exercicesDeposes: z.number(),
  moyenne: z.number().nullable(),
  relecturesEnAttente: z.number(),
  /** Additive extension (issue #41): how many of `presences` were added by the trainer. */
  presencesFormateur: z.number().optional(),
  /** Additive extension (contract v1.3, RG16): the average includes a provisional grade. */
  moyenneProvisoire: z.boolean().optional(),
})
export type TableauLigne = z.infer<typeof tableauLigneSchema>
