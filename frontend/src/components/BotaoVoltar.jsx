import { useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

export function BotaoVoltar({ destino = '/', className = '' }) {
  const navigate = useNavigate()

  function voltar() {
    if (window.history.state?.idx > 0) navigate(-1)
    else navigate(destino, { replace: true })
  }

  return <button type="button" onClick={voltar}
    className={`inline-flex min-h-11 items-center gap-2 rounded-lg px-3 text-sm text-secondary transition-colors hover:bg-surface-container focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${className}`}>
    <ArrowLeft className="size-4 shrink-0" aria-hidden="true" /> Voltar
  </button>
}
