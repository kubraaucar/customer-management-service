export type Role = 'ADMIN' | 'VIEWER'

/** A successful login, as returned by /api/auth/login and /api/auth/demo. */
export interface Session {
  token: string
  /** ISO time; after it the backend no longer accepts the token. */
  expiresAt: string
  username: string
  role: Role
}

const STORAGE_KEY = 'customer-management.session'

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Yönetici',
  VIEWER: 'Demo',
}

export function isExpired(session: Session, now: number = Date.now()): boolean {
  const expiresAt = Date.parse(session.expiresAt)
  return Number.isNaN(expiresAt) || expiresAt <= now
}

function isSession(value: unknown): value is Session {
  if (!value || typeof value !== 'object') return false
  const session = value as Record<string, unknown>
  return (
    typeof session.token === 'string' &&
    typeof session.expiresAt === 'string' &&
    typeof session.username === 'string' &&
    (session.role === 'ADMIN' || session.role === 'VIEWER')
  )
}

/** The login saved in this browser, or null if there is none or it has expired. */
export function loadSession(now: number = Date.now()): Session | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (!stored) return null
    const session: unknown = JSON.parse(stored)
    if (isSession(session) && !isExpired(session, now)) return session
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Storage blocked (e.g. private mode) or damaged data: behave as logged out.
  }
  return null
}

/** Remembers the login so a page refresh does not log the user out. */
export function saveSession(session: Session): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  } catch {
    // Storage blocked: the login still works until the page is refreshed.
  }
}

export function clearSession(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Nothing to clear.
  }
}
