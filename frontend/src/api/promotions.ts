import { http } from './http'
import { etudiantSchema, promotionSchema, type Etudiant, type Promotion } from './schemas'
import { z } from 'zod'

export async function listPromotions(): Promise<Promotion[]> {
  const { data } = await http.get('/promotions')
  return z.array(promotionSchema).parse(data)
}

export async function listStudents(promotionId: number): Promise<Etudiant[]> {
  const { data } = await http.get(`/promotions/${promotionId}/etudiants`)
  return z.array(etudiantSchema).parse(data)
}
