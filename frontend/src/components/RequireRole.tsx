import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useIdentityStore, type Role } from '../stores/identityStore'

/** Route guard: redirects to "/" when no identity was picked, or the role does not match. */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const identity = useIdentityStore((state) => state.identity)
  if (!identity || identity.role !== role) {
    return <Navigate to="/" replace />
  }
  return children
}
