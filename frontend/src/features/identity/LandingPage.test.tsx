import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach } from 'vitest'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { Layout } from '../../components/Layout'
import { RequireRole } from '../../components/RequireRole'
import { LandingPage } from './LandingPage'
import { TrainerHome } from '../trainer/TrainerHome'
import { StudentHome } from '../student/StudentHome'
import { ReviewerHome } from '../reviewer/ReviewerHome'
import { useIdentityStore } from '../../stores/identityStore'
import { demoPromotions, demoStudents } from './identity.handlers'

afterEach(() => {
  cleanup()
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
          { index: true, element: <LandingPage /> },
          {
            path: 'formateur',
            element: (
              <RequireRole role="FORMATEUR">
                <TrainerHome />
              </RequireRole>
            ),
          },
          {
            path: 'etudiant',
            element: (
              <RequireRole role="ETUDIANT">
                <StudentHome />
              </RequireRole>
            ),
          },
          {
            path: 'relecteur',
            element: (
              <RequireRole role="RELECTEUR">
                <ReviewerHome />
              </RequireRole>
            ),
          },
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

async function pickPromotion(user: ReturnType<typeof userEvent.setup>, name: string) {
  await user.click(screen.getByRole('combobox', { name: /promotion/i }))
  const option = await screen.findByRole('option', { name })
  await user.click(option)
}

test('should_pick_role_then_promotion_then_student_and_enter_as_student_EF10', async () => {
  const user = userEvent.setup()
  renderApp()

  await user.click(screen.getByRole('button', { name: 'Étudiant' }))
  await pickPromotion(user, demoPromotions[0]!.nom)

  const studentList = await screen.findByRole('radiogroup', { name: /étudiant/i })
  const firstStudent = demoStudents[0]!
  await user.click(within(studentList).getByRole('radio', { name: firstStudent.nom }))

  await user.click(screen.getByRole('button', { name: /entrer/i }))

  await waitFor(() => {
    expect(screen.getByRole('heading', { name: /espace étudiant/i })).toBeInTheDocument()
  })
  expect(useIdentityStore.getState().identity).toEqual({
    role: 'ETUDIANT',
    promotionId: demoPromotions[0]!.id,
    promotionNom: demoPromotions[0]!.nom,
    etudiantId: firstStudent.id,
    etudiantNom: firstStudent.nom,
  })
})

test('should_enter_as_formateur_with_only_a_promotion_EF10', async () => {
  const user = userEvent.setup()
  renderApp()

  await user.click(screen.getByRole('button', { name: 'Formateur' }))
  // No student roster for the trainer role.
  expect(screen.queryByRole('radiogroup', { name: /étudiant/i })).not.toBeInTheDocument()

  await pickPromotion(user, demoPromotions[0]!.nom)
  await user.click(screen.getByRole('button', { name: /entrer/i }))

  await waitFor(() => {
    expect(screen.getByRole('heading', { name: /espace formateur/i })).toBeInTheDocument()
  })
  expect(useIdentityStore.getState().identity).toEqual({
    role: 'FORMATEUR',
    promotionId: demoPromotions[0]!.id,
    promotionNom: demoPromotions[0]!.nom,
    etudiantId: undefined,
    etudiantNom: undefined,
  })
})

test('should_redirect_a_student_away_from_the_trainer_route_EF10', async () => {
  useIdentityStore.getState().setIdentity({
    role: 'ETUDIANT',
    promotionId: demoPromotions[0]!.id,
    promotionNom: demoPromotions[0]!.nom,
    etudiantId: demoStudents[0]!.id,
    etudiantNom: demoStudents[0]!.nom,
  })
  renderApp('/formateur')

  await waitFor(() => {
    expect(screen.getByRole('heading', { name: /rejoindre/i })).toBeInTheDocument()
  })
})
