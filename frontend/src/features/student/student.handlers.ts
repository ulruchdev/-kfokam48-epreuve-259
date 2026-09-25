import { http, HttpResponse } from 'msw'

export const demoSessionId = 7

export const demoMyExercises = [
  {
    id: 1,
    sessionId: demoSessionId,
    etudiantId: 1,
    etudiantNom: 'Amina Ndongo',
    lien: 'https://github.com/amina/tp-react',
    statut: 'RELU',
    // relecteurNom below is NOT part of the contract (RG7 forbids it) — kept here to prove
    // the zod schema strips unknown fields so it can never reach the UI.
    relecture: { note: 16, commentaire: 'Bon travail, attention aux clés de liste.', relecteurNom: 'Nom Secret' },
  },
  {
    id: 2,
    sessionId: demoSessionId,
    etudiantId: 1,
    etudiantNom: 'Amina Ndongo',
    lien: 'https://github.com/amina/tp-hooks',
    statut: 'EN_ATTENTE_RELECTURE',
    relecture: null,
  },
]

export const studentHandlers = [
  http.post('/api/presences', async ({ request }) => {
    const body = (await request.json()) as { code: string; etudiantId: number }
    if (body.code === 'EXPIRE1') {
      return HttpResponse.json(
        { code: 'CODE_EXPIRE', message: 'Le code de présence a expiré.' },
        { status: 410 },
      )
    }
    return HttpResponse.json(
      { id: 501, sessionId: demoSessionId, etudiantId: body.etudiantId, source: 'ETUDIANT' },
      { status: 201 },
    )
  }),
  http.post('/api/exercices', () => HttpResponse.json({ id: 1, statut: 'EN_ATTENTE_AFFECTATION' }, { status: 201 })),
  http.put('/api/exercices/:id', async ({ params, request }) => {
    const body = (await request.json()) as { lien: string }
    return HttpResponse.json({
      id: Number(params.id),
      sessionId: demoSessionId,
      etudiantId: 1,
      etudiantNom: 'Amina Ndongo',
      lien: body.lien,
      statut: 'EN_ATTENTE_AFFECTATION',
    })
  }),
  http.get('/api/etudiants/:etudiantId/exercices', () => HttpResponse.json(demoMyExercises)),
]
