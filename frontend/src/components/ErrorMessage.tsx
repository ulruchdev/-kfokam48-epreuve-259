import { ApiError } from '../api/http'
import styles from './ErrorMessage.module.css'

/** Shows the API's own `message` (French, imposed by the contract) — never a generic fallback. */
export function ErrorMessage({ error }: { error: unknown }) {
  const message =
    error instanceof ApiError
      ? error.message
      : error instanceof Error
        ? error.message
        : "Une erreur inattendue s'est produite."

  return (
    <p className={styles.error} role="alert">
      {message}
    </p>
  )
}
