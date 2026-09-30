import { X } from 'lucide-react'

export function Modal({ titulo, onClose, children }) {
  return (
    <div className="fixed inset-0 z-40 grid place-items-center bg-foreground/40 p-4" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <div role="dialog" aria-modal="true" aria-label={titulo} className="card-surface w-full max-w-md max-h-[92vh] overflow-y-auto p-6">
        <header className="mb-4 flex items-start justify-between gap-4">
          <h2 className="text-xl font-semibold">{titulo}</h2>
          <button onClick={onClose} aria-label="Fechar" className="btn btn-ghost btn-icon shrink-0"><X className="size-5" /></button>
        </header>
        {children}
      </div>
    </div>
  )
}
