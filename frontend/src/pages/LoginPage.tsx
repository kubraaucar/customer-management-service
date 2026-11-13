import { useState, type FormEvent } from 'react'
import { login, loginAsDemo } from '../api/auth'
import { errorMessage } from '../api/http'
import type { Session } from '../auth/session'

interface LoginPageProps {
  /** A message to show above the form, e.g. "your session has expired". */
  notice: string | null
  onLoggedIn: (session: Session) => void
}

export function LoginPage({ notice, onLoggedIn }: LoginPageProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState<'password' | 'demo' | null>(null)

  async function attempt(kind: 'password' | 'demo', action: () => Promise<Session>) {
    setBusy(kind)
    setError(null)
    try {
      onLoggedIn(await action())
    } catch (caught) {
      setError(errorMessage(caught))
      setBusy(null)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!username.trim() || !password) {
      setError('Kullanıcı adı ve şifre girin.')
      return
    }
    void attempt('password', () => login(username.trim(), password))
  }

  return (
    <div className="login-page">
      <main className="card login-card">
        <div className="brand">
          <img src="/favicon.svg" alt="" width="36" height="36" />
          <span>Müşteri Yönetimi</span>
        </div>
        <h1>Giriş yap</h1>

        {notice && !error && (
          <p className="alert alert-info" role="status">
            {notice}
          </p>
        )}
        {error && (
          <p className="alert alert-error" role="alert">
            {error}
          </p>
        )}

        <form className="form" onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="login-username">Kullanıcı adı</label>
            <input
              id="login-username"
              type="text"
              value={username}
              autoComplete="username"
              autoCapitalize="none"
              autoFocus
              onChange={(event) => setUsername(event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="login-password">Şifre</label>
            <input
              id="login-password"
              type="password"
              value={password}
              autoComplete="current-password"
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>
          <button type="submit" className="button button-primary button-block" disabled={busy !== null}>
            {busy === 'password' ? 'Giriş yapılıyor…' : 'Giriş yap'}
          </button>
        </form>

        <div className="divider" aria-hidden="true">
          <span>veya</span>
        </div>

        <button
          type="button"
          className="button button-block"
          disabled={busy !== null}
          onClick={() => void attempt('demo', loginAsDemo)}
        >
          {busy === 'demo' ? 'Giriş yapılıyor…' : 'Demo olarak dene'}
        </button>
        <p className="hint">
          Kayıt gerekmez. Demo hesabıyla müşterileri görüntüleyip arayabilirsiniz, ancak değişiklik yapamazsınız.
        </p>
      </main>
    </div>
  )
}
