import { useEffect, useState } from 'react'

function remainingSeconds(expirationAt: string): number {
  return Math.max(0, Math.floor((new Date(expirationAt).getTime() - Date.now()) / 1000))
}

function formatMmSs(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

/**
 * RG1: the code expires 15 minutes after opening — a live countdown against
 * expirationAt. Render with `key={expirationAt}` so a different session
 * remounts (and re-derives its initial value) instead of resetting state
 * from inside the effect.
 */
export function CodeCountdown({ expirationAt }: { expirationAt: string }) {
  const [seconds, setSeconds] = useState(() => remainingSeconds(expirationAt))

  useEffect(() => {
    const interval = setInterval(() => setSeconds(remainingSeconds(expirationAt)), 1000)
    return () => clearInterval(interval)
  }, [expirationAt])

  const expired = seconds <= 0

  return (
    <p
      className={`font-mono text-sm ${expired ? 'text-destructive' : 'text-chalk/70'}`}
      data-testid="code-countdown"
    >
      {expired ? 'Code expiré' : `Expire dans ${formatMmSs(seconds)}`}
    </p>
  )
}
