import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach } from 'vitest'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { Layout } from '../../components/Layout'
import { IdentityScreen } from './IdentityScreen'
import { TrainerHome } from '../trainer/TrainerHome'
import { StudentHome } from '../student/StudentHome'
import { ReviewerHome } from '../reviewer/ReviewerHome'
import { useIdentityStore } from '../../stores/identityStore'
import { demoPromotions, demoStudents } from './identity.handlers'

afterEach(() => {
  useIdentityStore.getState().clearIdentity()
})

function renderApp(initialPath = '/') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(
    [
      {
        path: '/',
        element: <Layout />,
        children: [
          { index: true, element: <IdentityScreen /> },
          { path: 'formateur', element: <TrainerHome /> },
          { path: 'etudiant', element: <StudentHome /> },
          { path: 'relecteur', element: <ReviewerHome /> },
        ],
      },
    ],
    { initialEntries: [initialPath] },
  )
  return render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

test('should_pick_promotion_then_student_then_role_and_enter_as_student_EF10', async () => {
  const user = userEvent.setup()
  renderApp()

  const promotionSelect = await screen.findByLabelText(/promotion/i)
  await user.selectOptions(promotionSelect, String(demoPromotions[0]!.id))

  const studentList = await screen.findByRole('radiogroup', { name: /étudiant/i })
  const firstStudent = demoStudents[0]!
  await user.click(within(studentList).getByRole('radio', { name: firstStudent.nom }))

  const roleGroup = await screen.findByRole('radiogroup', { name: /rôle/i })
  await user.click(within(roleGroup).getByRole('radio', { name: /étudiant/i }))

  await user.click(screen.getByRole('button', { name: /entrer/i }))

  await waitFor(() => {
    expect(screen.getByRole('heading', { name: /espace étudiant/i })).toBeInTheDocument()
  })
  expect(useIdentityStore.getState().identity).toEqual({
    promotionId: demoPromotions[0]!.id,
    promotionNom: demoPromotions[0]!.nom,
    etudiantId: firstStudent.id,
    etudiantNom: firstStudent.nom,
    role: 'ETUDIANT',
  })
})

test('should_enter_as_formateur_EF10', async () => {
  const user = userEvent.setup()
  renderApp()

  const promotionSelect = await screen.findByLabelText(/promotion/i)
  await user.selectOptions(promotionSelect, String(demoPromotions[0]!.id))

  const studentList = await screen.findByRole('radiogroup', { name: /étudiant/i })
  await user.click(within(studentList).getByRole('radio', { name: demoStudents[1]!.nom }))

  const roleGroup = await screen.findByRole('radiogroup', { name: /rôle/i })
  await user.click(within(roleGroup).getByRole('radio', { name: /formateur/i }))

  await user.click(screen.getByRole('button', { name: /entrer/i }))

  await waitFor(() => {
    expect(screen.getByRole('heading', { name: /espace formateur/i })).toBeInTheDocument()
  })
})
