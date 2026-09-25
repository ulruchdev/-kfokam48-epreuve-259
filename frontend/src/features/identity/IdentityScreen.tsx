import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import type { Role } from '../../stores/identityStore'
import { useIdentityStore } from '../../stores/identityStore'
import { usePromotions, useStudents } from './queries'
import styles from './IdentityScreen.module.css'

const roles: { value: Role; label: string; route: string }[] = [
  { value: 'FORMATEUR', label: 'Formateur', route: '/formateur' },
  { value: 'ETUDIANT', label: 'Étudiant', route: '/etudiant' },
  { value: 'RELECTEUR', label: 'Relecteur', route: '/relecteur' },
]

/** EF10: choose promotion, then name in that promotion's list, then role — no password (Q1). */
export function IdentityScreen() {
  const navigate = useNavigate()
  const setIdentity = useIdentityStore((state) => state.setIdentity)

  const [promotionId, setPromotionId] = useState<number | undefined>(undefined)
  const [etudiantId, setEtudiantId] = useState<number | undefined>(undefined)
  const [role, setRole] = useState<Role | undefined>(undefined)

  const promotions = usePromotions()
  const students = useStudents(promotionId)

  const selectedPromotion = promotions.data?.find((p) => p.id === promotionId)
  const selectedStudent = students.data?.find((e) => e.id === etudiantId)
  const selectedRole = roles.find((r) => r.value === role)

  const canEnter = Boolean(selectedPromotion && selectedStudent && selectedRole)

  function handlePromotionChange(value: string) {
    const id = value ? Number(value) : undefined
    setPromotionId(id)
    setEtudiantId(undefined)
    setRole(undefined)
  }

  function handleEnter() {
    if (!selectedPromotion || !selectedStudent || !selectedRole) return
    setIdentity({
      promotionId: selectedPromotion.id,
      promotionNom: selectedPromotion.nom,
      etudiantId: selectedStudent.id,
      etudiantNom: selectedStudent.nom,
      role: selectedRole.value,
    })
    navigate(selectedRole.route)
  }

  return (
    <section className={styles.screen}>
      <h1>Choisir son identité</h1>
      <p>Retrouvez votre promotion, puis votre nom dans la liste. Aucun mot de passe.</p>

      <div className={styles.field}>
        <label htmlFor="promotion-select" className={styles.label}>
          Promotion
        </label>
        {promotions.isLoading && <Loading label="Chargement des promotions…" />}
        {promotions.isError && <ErrorMessage error={promotions.error} />}
        {promotions.data && (
          <select
            id="promotion-select"
            className={styles.select}
            value={promotionId ?? ''}
            onChange={(event) => handlePromotionChange(event.target.value)}
          >
            <option value="" disabled>
              Sélectionner une promotion
            </option>
            {promotions.data.map((promotion) => (
              <option key={promotion.id} value={promotion.id}>
                {promotion.nom}
              </option>
            ))}
          </select>
        )}
      </div>

      {promotionId !== undefined && (
        <div className={styles.field}>
          <span className={styles.label} id="etudiant-label">
            Étudiant
          </span>
          {students.isLoading && <Loading label="Chargement des étudiants…" />}
          {students.isError && <ErrorMessage error={students.error} />}
          {students.data && (
            <div
              className={styles.roster}
              role="radiogroup"
              aria-labelledby="etudiant-label"
            >
              {students.data.map((student) => (
                <label key={student.id} className={styles.rosterRow}>
                  <input
                    type="radio"
                    name="etudiant"
                    value={student.id}
                    checked={etudiantId === student.id}
                    onChange={() => setEtudiantId(student.id)}
                  />
                  <span>{student.nom}</span>
                </label>
              ))}
            </div>
          )}
        </div>
      )}

      {etudiantId !== undefined && (
        <div className={styles.field}>
          <span className={styles.label} id="role-label">
            Rôle
          </span>
          <div className={styles.tabs} role="radiogroup" aria-labelledby="role-label">
            {roles.map((option) => (
              <label
                key={option.value}
                className={`${styles.tab} ${role === option.value ? styles.tabActive : ''}`}
              >
                <input
                  type="radio"
                  name="role"
                  value={option.value}
                  checked={role === option.value}
                  onChange={() => setRole(option.value)}
                />
                {option.label}
              </label>
            ))}
          </div>
        </div>
      )}

      <button
        type="button"
        className={styles.enter}
        disabled={!canEnter}
        onClick={handleEnter}
      >
        Entrer
      </button>
    </section>
  )
}
