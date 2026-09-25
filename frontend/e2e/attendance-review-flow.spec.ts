import { expect, test, type Page } from '@playwright/test'
import { formatAverage } from '../src/lib/format.js'
import { pickIdentity } from './helpers.js'

/**
 * End-to-end flow across three independent browser contexts (trainer, two
 * students — the second student then switches to the reviewer role from the
 * landing page, exactly as EF10 allows: any student can pick any role).
 *
 * Requires the full stack up: `docker compose up --build` (db + backend +
 * frontend on :5173). Demo data is promotion id 1 with six students
 * (docs/CAHIER_DES_CHARGES.md §3); names are read from the roster at
 * runtime rather than hard-coded, since only their count is guaranteed.
 *
 * The order below is deliberate: student A deposits their exercise while
 * they are the only student present, so the system cannot draw a reviewer
 * yet (RG15 — DEC-3) and the exercise stays EN_ATTENTE_AFFECTATION; student
 * B's attendance then retries the draw and B becomes the reviewer. This
 * exercises RG15's retry-on-new-presence, not just the EF4 happy path.
 */
test('trainer opens a session, a student deposits, two peers are drawn as reviewers, the grade is provisional then averaged, and the dashboard shows it', async ({
  browser,
}) => {
  const trainerCtx = await browser.newContext()
  const studentACtx = await browser.newContext()
  const studentBCtx = await browser.newContext()
  const studentCCtx = await browser.newContext()

  const trainerPage = await trainerCtx.newPage()
  const studentAPage = await studentACtx.newPage()
  const studentBPage = await studentBCtx.newPage()
  const studentCPage = await studentCCtx.newPage()

  // 1. Trainer opens the session (default 120 min duration) and gets the
  //    code, shown big with a live countdown (EF1, US-40).
  await pickIdentity(trainerPage, { studentIndex: 0, role: 'Formateur' })
  await trainerPage.getByLabel(/titre de la session/i).fill(`E2E ${Date.now()}`)
  await trainerPage.getByRole('button', { name: /ouvrir la session/i }).click()
  const code = (await trainerPage.getByTestId('session-code').textContent())?.trim()
  expect(code).toBeTruthy()
  await expect(trainerPage.getByTestId('code-countdown')).toContainText(/expire dans/i)

  // 2. Student A marks attendance (EF2) and deposits (EF3) while alone: no
  //    reviewer can be drawn yet (RG15).
  const studentAName = await pickIdentity(studentAPage, { studentIndex: 1, role: 'Étudiant' })
  await markAttendance(studentAPage, code!)
  const exerciseLink = `https://example.com/e2e-exercice-${Date.now()}`
  await studentAPage.getByLabel(/lien de l.exercice/i).fill(exerciseLink)
  await studentAPage.getByRole('button', { name: /déposer mon exercice/i }).click()
  await expect(studentAPage.getByTestId('deposit-confirmation')).toBeVisible()

  // 3. Students B and C mark attendance: each new attendance draws a missing
  //    reviewer, so B and C become A's two reviewers (RG5 revised, step 3).
  await pickIdentity(studentBPage, { studentIndex: 2, role: 'Étudiant' })
  await markAttendance(studentBPage, code!)
  await pickIdentity(studentCPage, { studentIndex: 3, role: 'Étudiant' })
  await markAttendance(studentCPage, code!)

  // 4. B renders first: A sees the grade, marked provisional (RG16).
  await renderReview(studentBPage, 2, exerciseLink, '12', 'Découpage clair, tests à compléter.')
  const exerciseCard = studentAPage.locator('[data-testid^="exercise-"]').filter({ hasText: exerciseLink })
  await studentAPage.reload()
  await expect(exerciseCard).toContainText('12/20')
  await expect(exerciseCard).toContainText(/provisoire/i)

  // 5. C renders: the retained grade is the average of both, no longer provisional.
  await renderReview(studentCPage, 3, exerciseLink, '15', 'Tests lisibles et utiles.')
  await studentAPage.reload()
  await expect(exerciseCard).toContainText('13,5/20')
  await expect(exerciseCard).not.toContainText(/provisoire/i)
  await expect(exerciseCard).toContainText('Tests lisibles et utiles.')

  // 6. The trainer reloads (no real-time refresh, §3) and sees student A's
  //    average (EF6), checked against the value computed by the API (F3, DEC-13).
  await trainerPage.reload()
  const tableau = await (await trainerPage.request.get('/api/tableau?promotionId=1')).json()
  const apiRow = tableau.find((row: { nom: string }) => row.nom === studentAName)
  expect(apiRow.moyenne).not.toBeNull()
  const dashboardRow = trainerPage
    .getByTestId('dashboard-table')
    .getByRole('row')
    .filter({ hasText: studentAName })
  await expect(dashboardRow.getByTestId('dashboard-average')).toHaveText(formatAverage(apiRow.moyenne))

  await trainerCtx.close()
  await studentACtx.close()
  await studentBCtx.close()
  await studentCCtx.close()
})

async function markAttendance(page: Page, code: string) {
  await page.getByLabel(/code de la séance/i).fill(code)
  await page.getByRole('button', { name: /valider ma présence/i }).click()
  await expect(page.getByTestId('attendance-confirmation')).toBeVisible()
}

/** The student switches to the reviewer role (EF10) and renders a grade + comment (EF5). */
async function renderReview(page: Page, studentIndex: number, exerciseLink: string, note: string, comment: string) {
  await page.getByRole('button', { name: /changer de rôle/i }).click()
  await pickIdentity(page, { studentIndex, role: 'Relecteur' })
  const reviewRow = page.locator('[data-testid^="review-row-"]').filter({ hasText: exerciseLink })
  await expect(reviewRow).toBeVisible()
  await reviewRow.getByLabel(/note/i).fill(note)
  await reviewRow.getByLabel(/commentaire/i).fill(comment)
  await reviewRow.getByRole('button', { name: /envoyer la relecture/i }).click()
  await expect(reviewRow.getByText(`${note}/20`)).toBeVisible()
}
