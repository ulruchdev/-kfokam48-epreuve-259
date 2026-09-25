import { useQuery } from '@tanstack/react-query'
import { listPromotions, listStudents } from '../../api/promotions'

export function usePromotions() {
  return useQuery({
    queryKey: ['promotions'],
    queryFn: listPromotions,
  })
}

export function useStudents(promotionId: number | undefined) {
  return useQuery({
    queryKey: ['etudiants', promotionId],
    queryFn: () => listStudents(promotionId as number),
    enabled: promotionId !== undefined,
  })
}
