import { http } from './http'
import { relectureAssigneeSchema, relectureSchema, type RelectureAssignee, type Relecture } from './schemas'
import { z } from 'zod'

export interface ReviewInput {
  note: number
  commentaire: string
  /** Known deviation from api/contrat.yaml (authoritative): the chosen student id, optional. */
  relecteurId?: number
}

export async function listAssignedReviews(relecteurId: number): Promise<RelectureAssignee[]> {
  const { data } = await http.get('/relectures', { params: { relecteurId } })
  return z.array(relectureAssigneeSchema).parse(data)
}

export async function submitReview(id: number, input: ReviewInput): Promise<void> {
  await http.post(`/relectures/${id}`, input)
}

export async function amendReview(id: number, input: ReviewInput): Promise<Relecture> {
  const { data } = await http.put(`/relectures/${id}`, input)
  return relectureSchema.parse(data)
}
