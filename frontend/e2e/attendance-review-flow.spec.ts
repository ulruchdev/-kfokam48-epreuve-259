import { expect, test } from '@playwright/test'
import { formatAverage } from '../src/lib/format'
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
test('trainer opens a session, a student deposits an exercise, a second student is drawn as reviewer and grades it, and the dashboard shows the average', async ({
  browser,
}) => {
  const trainerCtx = await browser.newContext()
  const studentACtx = await browser.newContext()
  const studentBCtx = await browser.newContext()

  const trainerPage = await trainerCtx.newPage()
  const studentAPage = await studentACtx.newPage()
  const studentBPage = await studentBCtx.newPage()

  // 1. Trainer opens the session (default 120 min duration) and gets the
  //    code, shown big with a live countdown (EF1, US-40).
  await pickIdentity(trainerPage, { studentIndex: 0, role: 'Formateur' })
  await trainerPage.getByLabel(/titre de la session/i).fill(`E2E ${Date.now()}`)
  await trainerPage.getByRole('button', { name: /ouvrir la session/i }).click()
  const code = (await trainerPage.getByTestId('session-code').textContent())?.trim()
  expect(code).toBeTruthy()
  await expect(trainerPage.getByTestId('code-countdown')).toContainText(/expire dans/i)

  // 2. Student A marks attendance with that code (EF2) and deposits their
  //    exercise link (EF3), alone in the session so far.
  const studentAName = await pickIdentity(studentAPage, { studentIndex: 1, role: 'Étudiant' })
  await studentAPage.getByLabel(/code de la séance/i).fill(code!)
  await studentAPage.getByRole('button', { name: /valider ma présence/i }).click()
  await expect(studentAPage.getByTestId('attendance-confirmation')).toBeVisible()

  const exerciseLink = `https://example.com/e2e-exercice-${Date.now()}`
  await studentAPage.getByLabel(/lien de l.exercice/i).fill(exerciseLink)
  await studentAPage.getByRole('button', { name: /déposer mon exercice/i }).click()
  await expect(studentAPage.getByTestId('deposit-confirmation')).toBeVisible()

  // 3. Student B marks attendance: the only other present student, so RG15
  //    retries the reviewer draw and assigns B.
  await pickIdentity(studentBPage, { studentIndex: 2, role: 'Étudiant' })
  await studentBPage.getByLabel(/code de la séance/i).fill(code!)
  await studentBPage.getByRole('button', { name: /valider ma présence/i }).click()
  await expect(studentBPage.getByTestId('attendance-confirmation')).toBeVisible()

  // 4. Student B switches to the reviewer role (header "Changer de rôle",
  //    back to the landing page) and grades A's exercise (EF5).
  await studentBPage.getByRole('button', { name: /changer de rôle/i }).click()
  await pickIdentity(studentBPage, { studentIndex: 2, role: 'Relecteur' })
  const reviewRow = studentBPage
    .locator('[data-testid^="review-row-"]')
    .filter({ hasText: exerciseLink })
  await expect(reviewRow).toBeVisible()
  await reviewRow.getByLabel(/note/i).fill('18')
  await reviewRow.getByLabel(/commentaire/i).fill('Bon travail, quelques points à revoir.')
  await reviewRow.getByRole('button', { name: /envoyer la relecture/i }).click()
  await expect(reviewRow.getByText('18/20')).toBeVisible()

  // 5. The trainer reloads (no real-time refresh, §3) and sees student A's
  //    average on the dashboard (EF6). The average covers every grade A ever
  //    received (DEC-8, the demo seed included), so the screen is checked
  //    against the value computed by the API, never recomputed here (F3).
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
})
