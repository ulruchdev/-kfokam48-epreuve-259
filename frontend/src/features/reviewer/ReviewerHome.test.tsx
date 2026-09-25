import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router-dom'
import { ReviewerHome } from './ReviewerHome'
import { useIdentityStore } from '../../stores/identityStore'
import { demoPromotions, demoStudents } from '../identity/identity.handlers'
import { demoAssignedReviews } from './reviewer.handlers'
import { server } from '../../test/msw/server'

beforeEach(() => {
  useIdentityStore.getState().setIdentity({
    promotionId: demoPromotions[0]!.id,
    promotionNom: demoPromotions[0]!.nom,
    etudiantId: demoStudents[0]!.id,
    etudiantNom: demoStudents[0]!.nom,
    role: 'RELECTEUR',
  })
})

afterEach(() => {
  cleanup()
  useIdentityStore.getState().clearIdentity()
})

function renderReviewer() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ReviewerHome />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

test('should_list_assigned_reviews_EF13', async () => {
  renderReviewer()

  const pending = demoAssignedReviews[0]!
  const done = demoAssignedReviews[1]!
  await waitFor(() => {
    expect(screen.getByText(pending.auteurNom)).toBeInTheDocument()
  })
  expect(screen.getByText(done.auteurNom)).toBeInTheDocument()
  expect(screen.getByText(pending.exerciceLien)).toBeInTheDocument()
})

test('should_render_grade_0_20_and_comment_for_a_rendered_review_EF5', async () => {
  renderReviewer()

  const done = demoAssignedReviews[1]!
  await waitFor(() => {
    expect(screen.getByText(`${done.note}/20`)).toBeInTheDocument()
  })
  expect(screen.getByText(done.commentaire)).toBeInTheDocument()
})

test('should_submit_a_pending_review_EF5', async () => {
  const user = userEvent.setup()
  renderReviewer()

  const pending = demoAssignedReviews[0]!
  const row = await screen.findByTestId(`review-row-${pending.id}`)

  await user.type(within(row).getByLabelText(/note/i), '14')
  await user.type(within(row).getByLabelText(/commentaire/i), 'Bon début, à approfondir.')
  await user.click(within(row).getByRole('button', { name: /envoyer la relecture/i }))

  await waitFor(() => {
    expect(within(row).getByText('14/20')).toBeInTheDocument()
  })
  expect(within(row).getByText('Bon début, à approfondir.')).toBeInTheDocument()
})

test('should_amend_a_rendered_review_EF11', async () => {
  const user = userEvent.setup()
  renderReviewer()

  const done = demoAssignedReviews[1]!
  const row = await screen.findByTestId(`review-row-${done.id}`)
  await within(row).findByText(`${done.note}/20`)

  await user.click(within(row).getByRole('button', { name: /modifier/i }))
  const noteField = within(row).getByLabelText(/note/i)
  await user.clear(noteField)
  await user.type(noteField, '20')
  await user.click(within(row).getByRole('button', { name: /enregistrer/i }))

  await waitFor(() => {
    expect(within(row).getByText('20/20')).toBeInTheDocument()
  })
})

test('should_show_api_error_when_amending_a_closed_session_EF11', async () => {
  server.use(
    http.put('/api/relectures/:id', () =>
      HttpResponse.json(
        { code: 'SESSION_CLOTUREE', message: 'La session est clôturée.' },
        { status: 409 },
      ),
    ),
  )
  const user = userEvent.setup()
  renderReviewer()

  const done = demoAssignedReviews[1]!
  const row = await screen.findByTestId(`review-row-${done.id}`)
  await within(row).findByText(`${done.note}/20`)

  await user.click(within(row).getByRole('button', { name: /modifier/i }))
  await user.click(within(row).getByRole('button', { name: /enregistrer/i }))

  await waitFor(() => {
    expect(within(row).getByRole('alert')).toHaveTextContent('La session est clôturée.')
  })
})
