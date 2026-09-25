import { Link, Outlet } from 'react-router-dom'
import { useIdentityStore } from '../stores/identityStore'
import styles from './Layout.module.css'

const roleLabel: Record<string, string> = {
  FORMATEUR: 'Formateur',
  ETUDIANT: 'Étudiant',
  RELECTEUR: 'Relecteur',
}

export function Layout() {
  const identity = useIdentityStore((state) => state.identity)
  const clearIdentity = useIdentityStore((state) => state.clearIdentity)

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <Link to="/" className={styles.wordmark}>
          KFOKAM48 <span className={styles.wordmarkSub}>registre</span>
        </Link>
        {identity && (
          <div className={styles.badge} data-testid="identity-badge">
            <span className={styles.badgeRole}>{roleLabel[identity.role]}</span>
            <span className={styles.badgeName}>{identity.etudiantNom}</span>
            <button type="button" className={styles.badgeSwitch} onClick={clearIdentity}>
              Changer
            </button>
          </div>
        )}
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  )
}
