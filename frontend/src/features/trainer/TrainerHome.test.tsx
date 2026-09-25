import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router-dom'
import { Toaster } from '@/components/ui/sonner'
import { TrainerHome } from './TrainerHome'
import { useIdentityStore } from '../../stores/identityStore'
import { demoPromotions, demoStudents } from '../identity/identity.handlers'
import { server } from '../../test/msw/server'

beforeEach(() => {
  useIdentityStore.getState().setIdentity({
    role: 'FORMATEUR',
    promotionId: demoPromotions[0]!.id,
    promotionNom: demoPromotions[0]!.nom,
  })
})

afterEach(() => {
  // Unmount before clearing identity: TrainerHome assumes identity is set.
  cleanup()
  useIdentityStore.getState().clearIdentity()
})

function renderTrainer() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <TrainerHome />
        <Toaster />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

async function openSession(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/titre de la session/i), 'Cours React')
  await user.click(screen.getByRole('button', { name: /ouvrir la session/i }))
  await screen.findByTestId('session-code')
}

test('should_open_session_and_show_code_big_EF1', async () => {
  const user = userEvent.setup()
  renderTrainer()

  await openSession(user)

  expect(screen.getByTestId('session-code')).toHaveTextContent('AB12CD')
})

test('should_default_duration_to_120_and_show_a_live_countdown_US40', async () => {
  const user = userEvent.setup()
  renderTrainer()

  expect(screen.getByLabelText(/durée/i)).toHaveValue(120)
  await openSession(user)

  await waitFor(() => {
    expect(screen.getByTestId('code-countdown')).toHaveTextContent(/expire dans/i)
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

  await openSession(user)

  await user.selectOptions(
    screen.getByLabelText(/ajouter une présence/i),
    String(demoStudents[2]!.id),
  )
  await user.click(screen.getByRole('button', { name: /ajouter/i }))

  await waitFor(() => {
    expect(screen.getByText(/ajouté par le formateur/i)).toBeInTheDocument()
  })
})

test('should_list_promotion_sessions_and_reopen_one_to_see_its_code_US40', async () => {
  server.use(
    http.get('/api/sessions', () =>
      HttpResponse.json([
        {
          id: 77,
          titre: 'Séance passée',
          ouvertureAt: '2026-09-20T09:00:00Z',
          expirationAt: '2026-09-20T09:15:00Z',
          finAt: '2026-09-20T11:00:00Z',
          statut: 'TERMINEE',
        },
      ]),
    ),
    http.get('/api/sessions/77', () =>
      HttpResponse.json({
        id: 77,
        promotionId: 1,
        titre: 'Séance passée',
        code: 'OLDC0D3',
        ouvertureAt: '2026-09-20T09:00:00Z',
        expirationAt: '2026-09-20T09:15:00Z',
        finAt: '2026-09-20T11:00:00Z',
        statut: 'TERMINEE',
      }),
    ),
  )
  const user = userEvent.setup()
  renderTrainer()

  const row = await screen.findByTestId('session-row-77')
  expect(row).toHaveTextContent('Séance passée')
  await user.click(within(row).getByRole('button', { name: /reprendre/i }))

  await waitFor(() => {
    expect(screen.getByTestId('session-code')).toHaveTextContent('OLDC0D3')
  })
})

test('should_show_fin_avant_ouverture_error_when_adjusting_end_time_US40', async () => {
  server.use(
    http.put('/api/sessions/:id', () =>
      HttpResponse.json(
        {
          code: 'FIN_AVANT_OUVERTURE',
          message: "L'heure de fin doit être postérieure à l'ouverture de la session.",
        },
        { status: 400 },
      ),
    ),
  )
  const user = userEvent.setup()
  renderTrainer()

  await openSession(user)

  await user.type(screen.getByLabelText(/nouvelle heure de fin/i), '2020-01-01T10:00')
  await user.click(screen.getByRole('button', { name: /mettre à jour la fin/i }))

  await waitFor(() => {
    expect(screen.getByRole('alert')).toHaveTextContent(
      "L'heure de fin doit être postérieure à l'ouverture de la session.",
    )
  })
})

test('should_close_session_only_after_confirming_the_dialog_EF8', async () => {
  const user = userEvent.setup()
  renderTrainer()

  await openSession(user)

  await user.click(screen.getByRole('button', { name: /clôturer la session/i }))
  const dialog = await screen.findByRole('dialog')
  expect(dialog).toHaveTextContent(/clôturer/i)

  await user.click(within(dialog).getByRole('button', { name: /annuler/i }))
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  expect(screen.getByTestId('session-code')).toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: /clôturer la session/i }))
  const dialogAgain = await screen.findByRole('dialog')
  await user.click(within(dialogAgain).getByRole('button', { name: /confirmer/i }))

  await waitFor(() => {
    expect(screen.getByText(/session clôturée/i)).toBeInTheDocument()
  })
  expect(screen.queryByRole('button', { name: /clôturer la session/i })).not.toBeInTheDocument()
})
