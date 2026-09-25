import { describe, expect, it } from 'vitest'
import { formatAverage } from './format'

// EF6 / F3: the average comes from the API; the UI only formats it (fr-FR, 2 decimals max).
describe('formatAverage', () => {
  it('shows an integer average without decimals', () => {
    expect(formatAverage(15)).toBe('15')
  })

  it('rounds a periodic average to two decimals with a French comma', () => {
    expect(formatAverage(14.333333333333334)).toBe('14,33')
  })

  it('keeps one significant decimal', () => {
    expect(formatAverage(12.5)).toBe('12,5')
  })

  it('shows a dash when the student has no grade', () => {
    expect(formatAverage(null)).toBe('—')
  })
})
