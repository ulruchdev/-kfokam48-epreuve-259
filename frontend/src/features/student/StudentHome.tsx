import { useIdentityStore } from '../../stores/identityStore'

/** Filled in by feat/us-15-ui-student (EF2, EF3, EF9, EF12). */
export function StudentHome() {
  const identity = useIdentityStore((state) => state.identity)
  return (
    <section>
      <h1>Espace étudiant</h1>
      <p>Bienvenue {identity?.etudiantNom}. Le marquage de présence arrive bientôt.</p>
    </section>
  )
}
