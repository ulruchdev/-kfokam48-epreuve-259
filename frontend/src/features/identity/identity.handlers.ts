import { http, HttpResponse } from 'msw'

/** Demo data shape (promotion id 1, six students) mirrored for tests, per docs/CAHIER_DES_CHARGES.md §3. */
export const demoPromotions = [{ id: 1, nom: 'Promotion 48' }]

export const demoStudents = [
  { id: 1, nom: 'Amina Ndongo' },
  { id: 2, nom: 'Baptiste Roux' },
  { id: 3, nom: 'Chloé Fotso' },
  { id: 4, nom: 'David Mballa' },
  { id: 5, nom: 'Élise Tchoumi' },
  { id: 6, nom: 'Farid Mensah' },
]

export const identityHandlers = [
  http.get('/api/promotions', () => HttpResponse.json(demoPromotions)),
  http.get('/api/promotions/:promotionId/etudiants', ({ params }) => {
    if (Number(params.promotionId) !== 1) {
      return HttpResponse.json(
        { code: 'PROMOTION_INCONNUE', message: 'Promotion inconnue.' },
        { status: 404 },
      )
    }
    return HttpResponse.json(demoStudents)
  }),
]
