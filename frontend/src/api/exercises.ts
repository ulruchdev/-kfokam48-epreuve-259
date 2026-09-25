import { http } from './http'
import {
  exerciceAvecRelectureSchema,
  exerciceDeposeSchema,
  exerciceSchema,
  type Exercice,
  type ExerciceAvecRelecture,
  type ExerciceDepose,
} from './schemas'
import { z } from 'zod'

export interface SubmitExerciseInput {
  sessionId: number
  etudiantId: number
  lien: string
}

export async function submitExercise(input: SubmitExerciseInput): Promise<ExerciceDepose> {
  const { data } = await http.post('/exercices', input)
  return exerciceDeposeSchema.parse(data)
}

export async function replaceExerciseLink(id: number, lien: string): Promise<Exercice> {
  const { data } = await http.put(`/exercices/${id}`, { lien })
  return exerciceSchema.parse(data)
}

export async function listSessionExercises(sessionId: number): Promise<Exercice[]> {
  const { data } = await http.get(`/sessions/${sessionId}/exercices`)
  return z.array(exerciceSchema).parse(data)
}

export async function listStudentExercises(
  etudiantId: number,
  sessionId?: number,
): Promise<ExerciceAvecRelecture[]> {
  const { data } = await http.get(`/etudiants/${etudiantId}/exercices`, {
    params: sessionId ? { sessionId } : undefined,
  })
  return z.array(exerciceAvecRelectureSchema).parse(data)
}
