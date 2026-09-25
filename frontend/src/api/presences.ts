import { http } from './http'
import { presenceSchema, type Presence } from './schemas'

export interface MarkPresenceInput {
  code: string
  etudiantId: number
}

export async function markPresence(input: MarkPresenceInput): Promise<Presence> {
  const { data } = await http.post('/presences', input)
  return presenceSchema.parse(data)
}

export async function addManualPresence(
  sessionId: number,
  etudiantId: number,
): Promise<Presence> {
  const { data } = await http.post(`/sessions/${sessionId}/presences`, { etudiantId })
  return presenceSchema.parse(data)
}
