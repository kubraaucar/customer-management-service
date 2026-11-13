import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearSession, isExpired, loadSession, saveSession, type Session } from './session'

/** A tiny in-memory stand-in for the browser's localStorage. */
function fakeStorage() {
  const data = new Map<string, string>()
  return {
    getItem: (key: string) => data.get(key) ?? null,
    setItem: (key: string, value: string) => void data.set(key, String(value)),
    removeItem: (key: string) => void data.delete(key),
    clear: () => data.clear(),
    get size() {
      return data.size
    },
  }
}

const NOW = Date.parse('2026-11-13T12:00:00Z')

const session: Session = {
  token: 'abc.def.ghi',
  expiresAt: '2026-11-13T20:00:00Z',
  username: 'admin',
  role: 'ADMIN',
}

let storage: ReturnType<typeof fakeStorage>

beforeEach(() => {
  storage = fakeStorage()
  vi.stubGlobal('localStorage', storage)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('session storage', () => {
  it('remembers a saved login', () => {
    saveSession(session)
    expect(loadSession(NOW)).toEqual(session)
  })

  it('forgets the login after logout', () => {
    saveSession(session)
    clearSession()
    expect(loadSession(NOW)).toBeNull()
  })

  it('drops an expired login', () => {
    saveSession({ ...session, expiresAt: '2026-11-13T11:59:59Z' })
    expect(loadSession(NOW)).toBeNull()
    expect(storage.size).toBe(0)
  })

  it('ignores damaged or unexpected data', () => {
    storage.setItem('customer-management.session', '{not json')
    expect(loadSession(NOW)).toBeNull()

    storage.setItem('customer-management.session', JSON.stringify({ ...session, role: 'ROOT' }))
    expect(loadSession(NOW)).toBeNull()
  })

  it('works as logged out when the browser blocks storage', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => {
        throw new Error('blocked')
      },
    })
    expect(loadSession(NOW)).toBeNull()
  })
})

describe('isExpired', () => {
  it('compares the expiry time with now', () => {
    expect(isExpired(session, NOW)).toBe(false)
    expect(isExpired(session, Date.parse('2026-11-13T20:00:00Z'))).toBe(true)
  })

  it('treats an unreadable time as expired', () => {
    expect(isExpired({ ...session, expiresAt: 'soon' }, NOW)).toBe(true)
  })
})
