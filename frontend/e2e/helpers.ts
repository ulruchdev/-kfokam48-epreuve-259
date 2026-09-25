import type { Page } from '@playwright/test'

/**
 * Landing page flow (US-39): role first (a card button labelled exactly
 * "Formateur" / "Étudiant" / "Relecteur"), then promotion (a Select — pick
 * the first option, demo data has exactly one promotion), then a name from
 * the roster if the role needs one, then "Entrer". Returns the picked
 * student's name (empty string for Formateur, who picks no name).
 */
export async function pickIdentity(
  page: Page,
  options: {
    studentIndex: number
    role: 'Formateur' | 'Étudiant' | 'Relecteur'
  },
): Promise<string> {
  await page.goto('/')
  await page.getByRole('button', { name: options.role, exact: true }).click()

  await page.getByRole('combobox', { name: /promotion/i }).click()
  await page.getByRole('option').first().click()

  let name = ''
  if (options.role !== 'Formateur') {
    const roster = page.getByRole('radiogroup', { name: /étudiant/i })
    const radio = roster.getByRole('radio').nth(options.studentIndex)
    name = (await radio.evaluate((el) => el.closest('label')?.textContent?.trim())) ?? ''
    await radio.check()
  }

  await page.getByRole('button', { name: /entrer/i }).click()
  return name
}
