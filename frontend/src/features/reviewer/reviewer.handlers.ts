import { http, HttpResponse } from 'msw'

export const demoAssignedReviews = [
  {
    id: 10,
    exerciceId: 2,
    exerciceLien: 'https://github.com/baptiste/tp-hooks',
    auteurNom: 'Baptiste Roux',
    rendue: false,
    note: null,
    commentaire: null,
  },
  {
    id: 11,
    exerciceId: 3,
    exerciceLien: 'https://github.com/chloe/tp-router',
    auteurNom: 'Chloé Fotso',
    rendue: true,
    note: 18,
    commentaire: 'Très propre, bravo.',
  },
]

export const reviewerHandlers = [
  http.get('/api/relectures', () => HttpResponse.json(demoAssignedReviews)),
  http.post('/api/relectures/:id', () => HttpResponse.json({}, { status: 200 })),
  http.put('/api/relectures/:id', async ({ params, request }) => {
    const body = (await request.json()) as { note: number; commentaire: string }
    return HttpResponse.json({
      id: Number(params.id),
      exerciceId: 3,
      relecteurId: 1,
      note: body.note,
      commentaire: body.commentaire,
      rendue: true,
    })
  }),
]
