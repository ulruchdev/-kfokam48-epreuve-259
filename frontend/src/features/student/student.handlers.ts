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
    // Contract v1.3 (RG16): two rendered reviews → final retained grade = their average.
    // relecteurNom below is NOT part of the contract (RG7 forbids it) — kept here to prove
    // the zod schema strips unknown fields so it can never reach the UI.
    evaluation: {
      note: 13.5,
      provisoire: false,
      commentaires: ['Bon travail, attention aux clés de liste.', 'Tests lisibles et utiles.'],
      relecteurNom: 'Nom Secret',
    },
  },
  {
    id: 2,
    sessionId: demoSessionId,
    etudiantId: 1,
    etudiantNom: 'Amina Ndongo',
    lien: 'https://github.com/amina/tp-hooks',
    statut: 'EN_ATTENTE_RELECTURE',
    // Only one of the two reviews rendered: grade shown, marked provisional (RG16).
    evaluation: { note: 12, provisoire: true, commentaires: ['Premier retour : découpage clair.'] },
  },
  {
    id: 3,
    sessionId: demoSessionId,
    etudiantId: 1,
    etudiantNom: 'Amina Ndongo',
    lien: 'https://github.com/amina/tp-context',
    statut: 'EN_ATTENTE_AFFECTATION',
    evaluation: null,
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
