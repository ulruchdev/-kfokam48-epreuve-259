import { expect, test } from '@playwright/test'
import { pickIdentity } from './helpers.js'

/**
 * Strict role guards (US-39): a route for one role redirects to "/" when the
 * current identity doesn't match, landing back on "Rejoindre" rather than a
 * blank or forbidden page.
 */
test('a student cannot open the trainer route', async ({ page }) => {
  await pickIdentity(page, { studentIndex: 1, role: 'Étudiant' })
  await expect(page).toHaveURL(/\/etudiant$/)

  await page.goto('/formateur')

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('heading', { name: /rejoindre/i })).toBeVisible()
})
