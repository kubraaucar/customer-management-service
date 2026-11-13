import { useEffect, useId, useRef, type ReactNode } from 'react'

interface ModalProps {
  title: string
  /** Called when the user closes the window (Esc key). Buttons inside call their own handlers. */
  onClose: () => void
  /** While true, Esc does not close the window (e.g. during saving). */
  busy?: boolean
  children: ReactNode
}

/** A centered window on top of the page, built on the browser's <dialog> element. */
export function Modal({ title, onClose, busy = false, children }: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = dialogRef.current
    // showModal() traps focus inside the window and dims the page behind it.
    if (dialog && !dialog.open) dialog.showModal()
  }, [])

  return (
    <dialog
      ref={dialogRef}
      className="modal"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault()
        if (!busy) onClose()
      }}
      // Some browsers close the window anyway after repeated Esc presses; keep React in sync.
      onClose={onClose}
    >
      <h2 id={titleId} className="modal-title">
        {title}
      </h2>
      {children}
    </dialog>
  )
}
