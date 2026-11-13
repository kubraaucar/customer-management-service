import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, errorMessage, onUnauthorized, request, setAuthToken, toApiError } from './http'

function problem(status: number, body: object): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
  setAuthToken(null)
  onUnauthorized(null)
})

describe('toApiError', () => {
  it('uses the Turkish detail and field errors from the backend', async () => {
    const error = await toApiError(
      problem(400, { status: 400, detail: 'Gönderilen bilgilerde hata var.', errors: { name: 'Ad boş olamaz.' } }),
    )
    expect(error.status).toBe(400)
    expect(error.message).toBe('Gönderilen bilgilerde hata var.')
    expect(error.fieldErrors).toEqual({ name: 'Ad boş olamaz.' })
  })

  it('falls back to a generic message when the body is not JSON', async () => {
    const error = await toApiError(new Response('<html>Internal error</html>', { status: 500 }))
    expect(error.message).toBe('Sunucuda beklenmeyen bir hata oluştu.')
    expect(error.fieldErrors).toEqual({})
  })

  it('says the server is unreachable when a proxy answers 502 (backend down)', async () => {
    const error = await toApiError(new Response('', { status: 502 }))
    expect(error.message).toBe('Sunucuya ulaşılamadı. Lütfen biraz sonra tekrar deneyin.')
  })
})

describe('request', () => {
  it('sends JSON and returns the parsed body', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const result = await request<{ ok: boolean }>('/api/x', { method: 'POST', body: '{}' })

    expect(result).toEqual({ ok: true })
    const headers = fetchMock.mock.calls[0][1].headers as Headers
    expect(headers.get('Content-Type')).toBe('application/json')
  })

  it('returns undefined for 204 No Content', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))
    await expect(request('/api/x', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('throws an ApiError with the backend message on an error status', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(problem(404, { detail: 'Müşteri bulunamadı: 1' })))
    await expect(request('/api/x')).rejects.toMatchObject({ status: 404, message: 'Müşteri bulunamadı: 1' })
  })

  it('explains when the server cannot be reached', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    await expect(request('/api/x')).rejects.toMatchObject({
      status: 0,
      message: 'Sunucuya ulaşılamadı. Lütfen biraz sonra tekrar deneyin.',
    })
  })

  it('passes cancellation through unchanged', async () => {
    const controller = new AbortController()
    controller.abort()
    const abortError = new DOMException('aborted', 'AbortError')
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(abortError))
    await expect(request('/api/x', { signal: controller.signal })).rejects.toBe(abortError)
  })
})

describe('login token', () => {
  it('is sent as a Bearer header once set, and not before', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(new Response('{}', { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    await request('/api/x')
    setAuthToken('abc.def.ghi')
    await request('/api/x')

    expect((fetchMock.mock.calls[0][1].headers as Headers).has('Authorization')).toBe(false)
    expect((fetchMock.mock.calls[1][1].headers as Headers).get('Authorization')).toBe('Bearer abc.def.ghi')
  })

  it('reports a rejected token (401) so the app can show the login page', async () => {
    const handler = vi.fn()
    onUnauthorized(handler)
    setAuthToken('expired.token')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(problem(401, { detail: 'Oturum geçersiz' })))

    await expect(request('/api/x')).rejects.toMatchObject({ status: 401 })
    expect(handler).toHaveBeenCalledOnce()
  })

  it('does not treat a wrong password (401 without a token) as an expired login', async () => {
    const handler = vi.fn()
    onUnauthorized(handler)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(problem(401, { detail: 'Kullanıcı adı veya şifre hatalı.' })))

    await expect(request('/api/auth/login', { method: 'POST', body: '{}' })).rejects.toMatchObject({
      message: 'Kullanıcı adı veya şifre hatalı.',
    })
    expect(handler).not.toHaveBeenCalled()
  })
})

describe('errorMessage', () => {
  it('shows ApiError messages and hides other errors', () => {
    expect(errorMessage(new ApiError(409, 'Çakışma'))).toBe('Çakışma')
    expect(errorMessage(new Error('internal detail'))).toBe('Beklenmeyen bir hata oluştu.')
  })
})
