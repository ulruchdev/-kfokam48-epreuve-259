import { useIdentityStore } from '../../stores/identityStore'

/** Filled in by feat/us-09-ui-reviewer (EF5, EF11, EF13). */
export function ReviewerHome() {
  const identity = useIdentityStore((state) => state.identity)
  return (
    <section>
      <h1>Espace relecteur</h1>
      <p>Bienvenue {identity?.etudiantNom}. Vos relectures assignées arrivent bientôt.</p>
    </section>
  )
}
