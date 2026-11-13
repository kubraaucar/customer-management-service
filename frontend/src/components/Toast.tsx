import { useEffect } from 'react'

export interface ToastMessage {
  /** A new id restarts the timer even when the text is the same. */
  id: number
  kind: 'success' | 'error'
  text: string
}

interface ToastProps {
  toast: ToastMessage | null
  onDismiss: () => void
}

/** A short message in the corner that disappears by itself after a few seconds. */
export function Toast({ toast, onDismiss }: ToastProps) {
  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(onDismiss, 4000)
    return () => clearTimeout(timer)
  }, [toast, onDismiss])

  return (
    <div className="toast-region" role="status" aria-live="polite">
      {toast && (
        <div className={`toast toast-${toast.kind}`}>
          <span>{toast.text}</span>
          <button type="button" className="toast-close" onClick={onDismiss} aria-label="Kapat">
            ×
          </button>
        </div>
      )}
    </div>
  )
}
