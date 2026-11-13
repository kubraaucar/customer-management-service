/** "2002-01-08" -> "08.01.2002". Works on the text so the time zone can never shift the day. */
export function formatDate(isoDate: string | null): string {
  if (!isoDate) return '—'
  const [year, month, day] = isoDate.split('-')
  return `${day}.${month}.${year}`
}

/** Local calendar date as yyyy-MM-dd (the format of <input type="date">). */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function fullName(customer: { name: string; surname: string }): string {
  return `${customer.name} ${customer.surname}`
}
