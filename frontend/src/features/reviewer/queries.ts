import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { amendReview, listAssignedReviews, submitReview, type ReviewInput } from '../../api/reviews'

export function useAssignedReviews(relecteurId: number) {
  return useQuery({
    queryKey: ['relectures-assignees', relecteurId],
    queryFn: () => listAssignedReviews(relecteurId),
  })
}

export function useSubmitReview(relecteurId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: ReviewInput }) => submitReview(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['relectures-assignees', relecteurId] })
    },
  })
}

export function useAmendReview(relecteurId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: ReviewInput }) => amendReview(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['relectures-assignees', relecteurId] })
    },
  })
}
