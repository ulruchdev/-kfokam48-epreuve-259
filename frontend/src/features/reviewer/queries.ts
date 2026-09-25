import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { amendReview, listAssignedReviews, submitReview, type ReviewInput } from '../../api/reviews'
import type { RelectureAssignee } from '../../api/schemas'

export function useAssignedReviews(relecteurId: number) {
  return useQuery({
    queryKey: ['relectures-assignees', relecteurId],
    queryFn: () => listAssignedReviews(relecteurId),
  })
}

/**
 * Patch the cached list in place instead of refetching: POST/PUT on a review
 * don't return the full RelectureAssignee shape, and the trainer's dashboard
 * (not this query) is the source of truth for anything aggregated.
 */
function patchCachedReview(
  queryClient: ReturnType<typeof useQueryClient>,
  relecteurId: number,
  id: number,
  input: ReviewInput,
) {
  queryClient.setQueryData<RelectureAssignee[]>(['relectures-assignees', relecteurId], (old) =>
    old?.map((review) =>
      review.id === id
        ? { ...review, rendue: true, note: input.note, commentaire: input.commentaire }
        : review,
    ),
  )
}

export function useSubmitReview(relecteurId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: ReviewInput }) => submitReview(id, input),
    onSuccess: (_data, { id, input }) => patchCachedReview(queryClient, relecteurId, id, input),
  })
}

export function useAmendReview(relecteurId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: ReviewInput }) => amendReview(id, input),
    onSuccess: (_data, { id, input }) => patchCachedReview(queryClient, relecteurId, id, input),
  })
}
