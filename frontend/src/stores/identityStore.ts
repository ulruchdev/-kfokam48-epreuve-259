import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type Role = 'FORMATEUR' | 'ETUDIANT' | 'RELECTEUR'

export interface Identity {
  promotionId: number
  promotionNom: string
  etudiantId: number
  etudiantNom: string
  role: Role
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
