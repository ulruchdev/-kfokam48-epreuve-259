import { useIdentityStore } from '../../stores/identityStore'

/** Filled in by feat/us-10-ui-trainer (EF1, EF6, EF7, EF8). */
export function TrainerHome() {
  const identity = useIdentityStore((state) => state.identity)
  return (
    <section>
      <h1>Espace formateur</h1>
      <p>Bienvenue {identity?.etudiantNom}. Le tableau de bord arrive bientôt.</p>
    </section>
  )
}
