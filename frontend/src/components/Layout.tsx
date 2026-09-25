import { Link, Outlet } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useIdentityStore } from '../stores/identityStore'

const roleLabel: Record<string, string> = {
  FORMATEUR: 'Formateur',
  ETUDIANT: 'Étudiant',
  RELECTEUR: 'Relecteur',
}

export function Layout() {
  const identity = useIdentityStore((state) => state.identity)
  const clearIdentity = useIdentityStore((state) => state.clearIdentity)

  return (
    <div className="flex min-h-svh flex-col">
      <header className="flex flex-wrap items-center justify-between gap-3 bg-slate px-5 py-3 text-chalk">
        <Link to="/" className="font-display text-lg font-semibold tracking-wide">
          KFOKAM48 <span className="font-body text-xs font-normal opacity-70">registre</span>
        </Link>
        {identity && (
          <div
            className="flex items-center gap-3 rounded-md bg-card px-3 py-1.5 text-card-foreground"
            data-testid="identity-badge"
          >
            <span className="border-r border-border pr-3 font-mono text-[0.7rem] text-ochre">
              {roleLabel[identity.role]}
            </span>
            <span className="text-sm font-semibold">
              {identity.etudiantNom ?? identity.promotionNom}
            </span>
            <Button variant="ghost" size="sm" className="h-auto px-2 py-1" onClick={clearIdentity}>
              <LogOut className="size-3.5" aria-hidden="true" />
              Changer de rôle
            </Button>
          </div>
        )}
      </header>
      <main className="mx-auto w-full max-w-3xl flex-1 px-5 py-6">
        <Outlet />
      </main>
    </div>
  )
}
