import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { TrainerHome } from './TrainerHome'
import { useIdentityStore } from '../../stores/identityStore'
import { demoPromotions, demoStudents } from '../identity/identity.handlers'

beforeEach(() => {
  useIdentityStore.getState().setIdentity({
    promotionId: demoPromotions[0]!.id,
    promotionNom: demoPromotions[0]!.nom,
    etudiantId: 99,
    etudiantNom: 'Formateur Test',
    role: 'FORMATEUR',
  })
})

afterEach(() => {
  useIdentityStore.getState().clearIdentity()
})

function renderTrainer() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <TrainerHome />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

test('should_open_session_and_show_code_big_EF1', async () => {
  const user = userEvent.setup()
  renderTrainer()

  await user.type(screen.getByLabelText(/titre de la session/i), 'Cours React')
  await user.click(screen.getByRole('button', { name: /ouvrir la session/i }))

  await waitFor(() => {
    expect(screen.getByTestId('session-code')).toHaveTextContent('AB12CD')
  })
})

test('should_show_dash_for_null_moyenne_in_dashboard_EF6', async () => {
  renderTrainer()

  const table = await screen.findByTestId('dashboard-table')
  await waitFor(() => {
    expect(table).toHaveTextContent(demoStudents[1]!.nom)
  })
  const secondRow = screen.getByTestId(`dashboard-row-${demoStudents[1]!.id}`)
  expect(secondRow).toHaveTextContent('—')
})

test('should_add_manual_presence_EF7', async () => {
  const user = userEvent.setup()
  renderTrainer()

  await user.type(screen.getByLabelText(/titre de la session/i), 'Cours React')
  await user.click(screen.getByRole('button', { name: /ouvrir la session/i }))
  await screen.findByTestId('session-code')

  await user.selectOptions(
    screen.getByLabelText(/ajouter une présence/i),
    String(demoStudents[2]!.id),
  )
  await user.click(screen.getByRole('button', { name: /ajouter/i }))

  await waitFor(() => {
    expect(screen.getByText(/ajouté par le formateur/i)).toBeInTheDocument()
  })
})

test('should_close_session_EF8', async () => {
  const user = userEvent.setup()
  renderTrainer()

  await user.type(screen.getByLabelText(/titre de la session/i), 'Cours React')
  await user.click(screen.getByRole('button', { name: /ouvrir la session/i }))
  await screen.findByTestId('session-code')

  await user.click(screen.getByRole('button', { name: /clôturer la session/i }))

  await waitFor(() => {
    expect(screen.getByText(/session clôturée/i)).toBeInTheDocument()
  })
  expect(screen.queryByRole('button', { name: /clôturer la session/i })).not.toBeInTheDocument()
})
