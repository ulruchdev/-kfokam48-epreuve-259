import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listStudents } from '../../api/promotions'
import { addManualPresence } from '../../api/presences'
import { closeSession, getSession, listSessions, openSession, type OpenSessionInput } from '../../api/sessions'
import { getTableau } from '../../api/dashboard'

export function useTrainerStudents(promotionId: number) {
  return useQuery({
    queryKey: ['etudiants', promotionId],
    queryFn: () => listStudents(promotionId),
  })
}

export function useSessions(promotionId: number) {
  return useQuery({
    queryKey: ['sessions', promotionId],
    queryFn: () => listSessions(promotionId),
  })
}

/** GET /api/sessions/{id} now returns the code too: used to recover a reopened session (US-40). */
export function useSessionDetail(sessionId: number | undefined) {
  return useQuery({
    queryKey: ['session-detail', sessionId],
    queryFn: () => getSession(sessionId as number),
    enabled: sessionId !== undefined,
  })
}

export function useOpenSession() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: OpenSessionInput) => openSession(input),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: ['sessions', variables.promotionId] })
    },
  })
}

export function useTableau(promotionId: number) {
  return useQuery({
    queryKey: ['tableau', promotionId],
    queryFn: () => getTableau(promotionId),
  })
}

export function useAddManualPresence(promotionId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ sessionId, etudiantId }: { sessionId: number; etudiantId: number }) =>
      addManualPresence(sessionId, etudiantId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tableau', promotionId] })
    },
  })
}

export function useCloseSession(promotionId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (sessionId: number) => closeSession(sessionId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['sessions', promotionId] })
    },
  })
}
