import { describe, expect, it } from 'vitest'
import { formatDate, fullName, toIsoDate } from './format'

describe('formatDate', () => {
  it('shows an ISO date in Turkish day.month.year order', () => {
    expect(formatDate('2002-01-08')).toBe('08.01.2002')
  })

  it('shows a dash when there is no date', () => {
    expect(formatDate(null)).toBe('—')
  })
})

describe('toIsoDate', () => {
  it('pads month and day with zeros', () => {
    expect(toIsoDate(new Date(2024, 2, 5))).toBe('2024-03-05')
  })
})

describe('fullName', () => {
  it('joins name and surname', () => {
    expect(fullName({ name: 'Kübra', surname: 'Uçar' })).toBe('Kübra Uçar')
  })
})
