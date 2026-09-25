import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { markPresence, type MarkPresenceInput } from '../../api/presences'
import {
  listStudentExercises,
  replaceExerciseLink,
  submitExercise,
  type SubmitExerciseInput,
} from '../../api/exercises'

export function useMarkPresence() {
  return useMutation({
    mutationFn: (input: MarkPresenceInput) => markPresence(input),
  })
}

export function useMyExercises(etudiantId: number) {
  return useQuery({
    queryKey: ['mes-exercices', etudiantId],
    queryFn: () => listStudentExercises(etudiantId),
  })
}

export function useSubmitExercise(etudiantId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: SubmitExerciseInput) => submitExercise(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['mes-exercices', etudiantId] })
    },
  })
}

export function useReplaceExerciseLink(etudiantId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, lien }: { id: number; lien: string }) => replaceExerciseLink(id, lien),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['mes-exercices', etudiantId] })
    },
  })
}
