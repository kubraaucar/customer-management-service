const API_BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '')

/** An error answer from the backend, already turned into a message we can show the user. */
export class ApiError extends Error {
  /** HTTP status code, or 0 when the server could not be reached at all. */
  readonly status: number
  /** Per-field messages from a 400 validation error, e.g. { name: "Ad boş olamaz." }. */
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const UNREACHABLE_MESSAGE = 'Sunucuya ulaşılamadı. Lütfen biraz sonra tekrar deneyin.'

function fallbackMessage(status: number): string {
  if (status === 404) return 'Kayıt bulunamadı.'
  if (status === 409) return 'İstek mevcut kayıtlarla çakışıyor.'
  // 502/503/504 come from a proxy in front of the backend (Vite in development) when the backend is down.
  if (status === 502 || status === 503 || status === 504) return UNREACHABLE_MESSAGE
  if (status >= 500) return 'Sunucuda beklenmeyen bir hata oluştu.'
  return `İstek başarısız oldu (HTTP ${status}).`
}

/** Reads the backend's ProblemDetail body ({ detail, errors }) and turns it into an ApiError. */
export async function toApiError(response: Response): Promise<ApiError> {
  let detail: string | undefined
  let fieldErrors: Record<string, string> = {}
  try {
    const body: unknown = await response.json()
    if (body && typeof body === 'object') {
      const problem = body as { detail?: unknown; errors?: unknown }
      if (typeof problem.detail === 'string' && problem.detail) detail = problem.detail
      if (problem.errors && typeof problem.errors === 'object') {
        fieldErrors = problem.errors as Record<string, string>
      }
    }
  } catch {
    // Not JSON (e.g. a proxy error page); fall back to a generic message.
  }
  return new ApiError(response.status, detail ?? fallbackMessage(response.status), fieldErrors)
}

/** Calls the backend and returns the parsed JSON body (undefined for 204 No Content). */
export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body !== undefined) headers.set('Content-Type', 'application/json')

  let response: Response
  try {
    response = await fetch(API_BASE + path, { ...init, headers })
  } catch (error) {
    if (init.signal?.aborted) throw error
    throw new ApiError(0, UNREACHABLE_MESSAGE)
  }

  if (!response.ok) throw await toApiError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

/** A message for any error, suitable for showing to the user. */
export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Beklenmeyen bir hata oluştu.'
}
