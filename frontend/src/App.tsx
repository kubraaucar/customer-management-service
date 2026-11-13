import { useCallback, useEffect, useLayoutEffect, useState } from 'react'
import { onUnauthorized, setAuthToken } from './api/http'
import { clearSession, loadSession, saveSession, type Session } from './auth/session'
import { CustomersPage } from './pages/CustomersPage'
import { LoginPage } from './pages/LoginPage'

const EXPIRED_MESSAGE = 'Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın.'
/** setTimeout cannot wait longer than this (about 24 days); longer waits would fire at once. */
const MAX_TIMER_DELAY = 2 ** 31 - 1

/** Shows the login page or, once logged in, the customer page. */
export default function App() {
  const [session, setSession] = useState<Session | null>(() => loadSession())
  const [notice, setNotice] = useState<string | null>(null)

  // Layout effects run before the customer page's data effects, so its first request already has the token.
  useLayoutEffect(() => {
    setAuthToken(session?.token ?? null)
  }, [session])

  const logOut = useCallback((message: string | null = null) => {
    clearSession()
    setSession(null)
    setNotice(message)
  }, [])

  // The backend rejected the token (e.g. it expired or the server key changed).
  useEffect(() => {
    onUnauthorized(() => logOut(EXPIRED_MESSAGE))
    return () => onUnauthorized(null)
  }, [logOut])

  // Log out by ourselves at the moment the token expires.
  useEffect(() => {
    if (!session) return
    const delay = Math.min(Math.max(Date.parse(session.expiresAt) - Date.now(), 0), MAX_TIMER_DELAY)
    const timer = setTimeout(() => logOut(EXPIRED_MESSAGE), delay)
    return () => clearTimeout(timer)
  }, [session, logOut])

  function handleLoggedIn(next: Session) {
    saveSession(next)
    setNotice(null)
    setSession(next)
  }

  if (!session) return <LoginPage notice={notice} onLoggedIn={handleLoggedIn} />
  return <CustomersPage session={session} onLogout={() => logOut()} />
}
