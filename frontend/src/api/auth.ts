import type { Session } from '../auth/session'
import { request } from './http'

export function login(username: string, password: string): Promise<Session> {
  return request<Session>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

/** Read-only login without a password. */
export function loginAsDemo(): Promise<Session> {
  return request<Session>('/api/auth/demo', { method: 'POST' })
}
