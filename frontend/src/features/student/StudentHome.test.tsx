import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router-dom'
import { StudentHome } from './StudentHome'
import { useIdentityStore } from '../../stores/identityStore'
import { demoPromotions, demoStudents } from '../identity/identity.handlers'
import { demoMyExercises } from './student.handlers'
import { server } from '../../test/msw/server'

beforeEach(() => {
  useIdentityStore.getState().setIdentity({
    promotionId: demoPromotions[0]!.id,
    promotionNom: demoPromotions[0]!.nom,
    etudiantId: demoStudents[0]!.id,
    etudiantNom: demoStudents[0]!.nom,
    role: 'ETUDIANT',
  })
})

afterEach(() => {
  cleanup()
  useIdentityStore.getState().clearIdentity()
})

function renderStudent() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <StudentHome />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

test('should_mark_attendance_with_code_and_reveal_deposit_form_EF2', async () => {
  const user = userEvent.setup()
  renderStudent()

  await user.type(screen.getByLabelText(/code de la séance/i), 'AB12CD')
  await user.click(screen.getByRole('button', { name: /valider ma présence/i }))

  await waitFor(() => {
    expect(screen.getByTestId('attendance-confirmation')).toBeInTheDocument()
  })
  expect(screen.getByLabelText(/lien de l.exercice/i)).toBeInTheDocument()
})

test('should_show_api_message_when_code_expired_EF2', async () => {
  const user = userEvent.setup()
  renderStudent()

  await user.type(screen.getByLabelText(/code de la séance/i), 'EXPIRE1')
  await user.click(screen.getByRole('button', { name: /valider ma présence/i }))

  await waitFor(() => {
    expect(screen.getByRole('alert')).toHaveTextContent('Le code de présence a expiré.')
  })
})

test('should_submit_then_replace_exercise_link_EF3_EF12', async () => {
  const user = userEvent.setup()
  renderStudent()

  await user.type(screen.getByLabelText(/code de la séance/i), 'AB12CD')
  await user.click(screen.getByRole('button', { name: /valider ma présence/i }))
  await screen.findByTestId('attendance-confirmation')

  const linkInput = screen.getByLabelText(/lien de l.exercice/i)
  await user.type(linkInput, 'https://github.com/amina/tp-router')
  await user.click(screen.getByRole('button', { name: /déposer mon exercice/i }))

  await waitFor(() => {
    expect(screen.getByRole('button', { name: /remplacer le lien/i })).toBeInTheDocument()
  })

  await user.clear(linkInput)
  await user.type(linkInput, 'https://github.com/amina/tp-router-v2')
  await user.click(screen.getByRole('button', { name: /remplacer le lien/i }))

  await waitFor(() => {
    expect(screen.getByTestId('deposit-confirmation')).toHaveTextContent(/mis à jour|remplacé/i)
  })
})

test('should_list_my_exercises_with_grade_and_never_a_reviewer_name_EF9_RG7', async () => {
  renderStudent()

  const gradedExercise = demoMyExercises[0]!
  const relecture = gradedExercise.relecture!
  await waitFor(() => {
    expect(screen.getByText(relecture.commentaire)).toBeInTheDocument()
  })
  expect(screen.getByText(`${relecture.note}/20`)).toBeInTheDocument()
  expect(screen.queryByText('Nom Secret')).not.toBeInTheDocument()
  expect(document.body.textContent).not.toMatch(/relecteur/i)
})

test('should_show_api_error_when_deposit_is_rejected_EF3', async () => {
  server.use(
    http.post('/api/exercices', () =>
      HttpResponse.json(
        { code: 'PRESENCE_REQUISE', message: 'Vous devez marquer votre présence avant de déposer.' },
        { status: 400 },
      ),
    ),
  )
  const user = userEvent.setup()
  renderStudent()

  await user.type(screen.getByLabelText(/code de la séance/i), 'AB12CD')
  await user.click(screen.getByRole('button', { name: /valider ma présence/i }))
  await screen.findByTestId('attendance-confirmation')

  await user.type(screen.getByLabelText(/lien de l.exercice/i), 'https://github.com/amina/tp')
  await user.click(screen.getByRole('button', { name: /déposer mon exercice/i }))

  await waitFor(() => {
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Vous devez marquer votre présence avant de déposer.',
    )
  })
})
