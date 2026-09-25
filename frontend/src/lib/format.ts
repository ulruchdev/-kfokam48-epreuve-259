const averageFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 })

/**
 * EF6 / F3: formats the average computed by the API — never recomputes it.
 * 14.333… → "14,33", 15 → "15", null (no grade yet) → "—".
 */
export function formatAverage(average: number | null | undefined): string {
  return average == null ? '—' : averageFormat.format(average)
}
