import { afterEach, describe, expect, it, vi } from 'vitest'
import { listCustomers, setCustomerActive } from './customers'

function stubFetch(body: unknown = { content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } }) {
  const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status: 200 }))
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function calledUrl(fetchMock: ReturnType<typeof vi.fn>): URL {
  return new URL(fetchMock.mock.calls[0][0] as string, 'http://localhost')
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('listCustomers', () => {
  it('sends paging, sorting with a stable tie-breaker and the trimmed search text', async () => {
    const fetchMock = stubFetch()

    await listCustomers({ search: '  kübra ', page: 2, size: 10, sort: { field: 'surname', direction: 'desc' } })

    const url = calledUrl(fetchMock)
    expect(url.pathname).toBe('/api/customers')
    expect(url.searchParams.get('page')).toBe('2')
    expect(url.searchParams.get('size')).toBe('10')
    expect(url.searchParams.getAll('sort')).toEqual(['surname,desc', 'citizenNumber,asc'])
    expect(url.searchParams.get('search')).toBe('kübra')
  })

  it('leaves out an empty search and does not repeat the TC sort', async () => {
    const fetchMock = stubFetch()

    await listCustomers({ search: '   ', page: 0, size: 10, sort: { field: 'citizenNumber', direction: 'asc' } })

    const url = calledUrl(fetchMock)
    expect(url.searchParams.has('search')).toBe(false)
    expect(url.searchParams.getAll('sort')).toEqual(['citizenNumber,asc'])
  })
})

describe('setCustomerActive', () => {
  it('sends PATCH /activation with the new state', async () => {
    const fetchMock = stubFetch({ id: 'abc', active: false })

    await setCustomerActive('abc', false)

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/customers/abc/activation')
    expect(init.method).toBe('PATCH')
    expect(init.body).toBe('{"active":false}')
  })
})
