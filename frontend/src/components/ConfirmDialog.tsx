import { useState, type ReactNode } from 'react'
import { errorMessage } from '../api/http'
import { Modal } from './Modal'

interface ConfirmDialogProps {
  title: string
  children: ReactNode
  confirmLabel: string
  busyLabel: string
  /** Does the work. If it throws, the message is shown and the window stays open. */
  onConfirm: () => Promise<void>
  onClose: () => void
}

export function ConfirmDialog({ title, children, confirmLabel, busyLabel, onConfirm, onClose }: ConfirmDialogProps) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleConfirm() {
    setBusy(true)
    setError(null)
    try {
      await onConfirm()
    } catch (caught) {
      setError(errorMessage(caught))
      setBusy(false)
    }
  }

  return (
    <Modal title={title} onClose={onClose} busy={busy}>
      <div className="modal-body">{children}</div>
      {error && (
        <p className="alert alert-error" role="alert">
          {error}
        </p>
      )}
      <div className="modal-actions">
        {/* Focus starts on "Vazgeç" so pressing Enter by accident does not delete anything. */}
        <button type="button" className="button" onClick={onClose} disabled={busy} autoFocus>
          Vazgeç
        </button>
        <button type="button" className="button button-danger" onClick={handleConfirm} disabled={busy}>
          {busy ? busyLabel : confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
