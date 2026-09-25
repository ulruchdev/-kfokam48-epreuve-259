import { http, HttpResponse } from 'msw'
import { demoStudents } from '../identity/identity.handlers'

export const demoTableau = demoStudents.map((student, index) => ({
  etudiantId: student.id,
  nom: student.nom,
  presences: index === 0 ? 1 : 0,
  exercicesDeposes: index === 0 ? 1 : 0,
  moyenne: index === 0 ? 15 : null,
  relecturesEnAttente: 0,
  // Contract v1.3 (RG16): the first student's average includes a provisional grade.
  moyenneProvisoire: index === 0,
}))

function inMinutes(minutes: number): string {
  return new Date(Date.now() + minutes * 60_000).toISOString()
}

export const trainerHandlers = [
  http.post('/api/sessions', async ({ request }) => {
    const body = (await request.json()) as { titre: string; promotionId: number }
    return HttpResponse.json(
      {
        id: 42,
        code: 'AB12CD',
        ouvertureAt: new Date().toISOString(),
        expirationAt: inMinutes(15),
        finAt: inMinutes(120),
        statut: 'OUVERTE',
        titre: body.titre,
      },
      { status: 201 },
    )
  }),
  http.get('/api/sessions', () => HttpResponse.json([])),
  http.get('/api/sessions/:id', ({ params }) =>
    HttpResponse.json({
      id: Number(params.id),
      promotionId: 1,
      titre: 'Session recouvrée',
      code: 'XY99ZZ',
      ouvertureAt: new Date().toISOString(),
      expirationAt: inMinutes(15),
      finAt: inMinutes(120),
      statut: 'OUVERTE',
    }),
  ),
  http.put('/api/sessions/:id', async ({ params, request }) => {
    const body = (await request.json()) as { finAt: string }
    return HttpResponse.json({
      id: Number(params.id),
      promotionId: 1,
      titre: 'Cours React',
      code: 'AB12CD',
      ouvertureAt: new Date().toISOString(),
      expirationAt: inMinutes(15),
      finAt: body.finAt,
      statut: 'OUVERTE',
    })
  }),
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
