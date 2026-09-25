import { http } from './http'
import {
  sessionDetailSchema,
  sessionOuvertureSchema,
  sessionResumeSchema,
  type SessionDetail,
  type SessionOuverture,
  type SessionResume,
} from './schemas'
import { z } from 'zod'

export interface OpenSessionInput {
  titre: string
  promotionId: number
  dureeMinutes?: number
}

export async function openSession(input: OpenSessionInput): Promise<SessionOuverture> {
  const { data } = await http.post('/sessions', input)
  return sessionOuvertureSchema.parse(data)
}

export async function listSessions(promotionId: number): Promise<SessionResume[]> {
  const { data } = await http.get('/sessions', { params: { promotionId } })
  return z.array(sessionResumeSchema).parse(data)
}

export async function getSession(id: number): Promise<SessionDetail> {
  const { data } = await http.get(`/sessions/${id}`)
  return sessionDetailSchema.parse(data)
}

export async function closeSession(id: number): Promise<void> {
  await http.post(`/sessions/${id}/cloture`)
}

export async function setSessionEnd(id: number, finAt: string): Promise<SessionDetail> {
  const { data } = await http.put(`/sessions/${id}`, { finAt })
  return sessionDetailSchema.parse(data)
}
