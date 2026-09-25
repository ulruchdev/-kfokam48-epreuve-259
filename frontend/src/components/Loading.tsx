import styles from './Loading.module.css'

export function Loading({ label = 'Chargement…' }: { label?: string }) {
  return (
    <p className={styles.loading} role="status" aria-live="polite">
      {label}
    </p>
  )
}
