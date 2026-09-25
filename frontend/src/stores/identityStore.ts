import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type Role = 'FORMATEUR' | 'ETUDIANT' | 'RELECTEUR'

/**
 * The trainer only ever picks a promotion (landing page, US-39): a session
 * belongs to a promotion, not to one named student. Étudiant/Relecteur also
 * pick their name in that promotion's roster (EF10, Q1: no password).
 */
export interface Identity {
  role: Role
  promotionId: number
  promotionNom: string
  etudiantId?: number
  etudiantNom?: string
}

interface IdentityState {
  identity: Identity | null
  setIdentity: (identity: Identity) => void
  clearIdentity: () => void
}

export const useIdentityStore = create<IdentityState>()(
  persist(
    (set) => ({
      identity: null,
      setIdentity: (identity) => set({ identity }),
      clearIdentity: () => set({ identity: null }),
    }),
    { name: 'kfokam48-identity' },
  ),
)
