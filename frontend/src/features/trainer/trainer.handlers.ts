import { http, HttpResponse } from 'msw'
import { demoStudents } from '../identity/identity.handlers'

export const demoTableau = demoStudents.map((student, index) => ({
  etudiantId: student.id,
  nom: student.nom,
  presences: index === 0 ? 1 : 0,
  exercicesDeposes: index === 0 ? 1 : 0,
  moyenne: index === 0 ? 15 : null,
  relecturesEnAttente: 0,
}))

export const trainerHandlers = [
  http.post('/api/sessions', async ({ request }) => {
    const body = (await request.json()) as { titre: string; promotionId: number }
    return HttpResponse.json(
      {
        id: 42,
        code: 'AB12CD',
        ouvertureAt: '2026-09-25T09:00:00Z',
        expirationAt: '2026-09-25T09:15:00Z',
        finAt: '2026-09-25T11:00:00Z',
        statut: 'OUVERTE',
        titre: body.titre,
      },
      { status: 201 },
    )
  }),
  http.get('/api/sessions', () => HttpResponse.json([])),
  http.get('/api/tableau', () => HttpResponse.json(demoTableau)),
  http.post('/api/sessions/:id/presences', async ({ params, request }) => {
    const body = (await request.json()) as { etudiantId: number }
    return HttpResponse.json(
      { id: 100, sessionId: Number(params.id), etudiantId: body.etudiantId, source: 'FORMATEUR' },
      { status: 201 },
    )
  }),
  http.post('/api/sessions/:id/cloture', () => new HttpResponse(null, { status: 204 })),
]
