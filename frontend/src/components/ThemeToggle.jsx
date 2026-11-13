import { Moon, Sun } from 'lucide-react'
import { useTema } from '../lib/useTema.js'

export function ThemeToggle() {
  const { tema, preferencia, alternar, disponivel } = useTema()
  const escuro = tema === 'escuro'
  return (
    <button
      type="button"
      onClick={alternar}
      disabled={!disponivel}
      className="btn btn-outline btn-icon"
      aria-label={escuro ? 'Ativar tema claro' : 'Ativar tema escuro'}
      title={`${preferencia === 'automatico' ? 'Automático ativo. ' : ''}${escuro ? 'Ativar tema claro' : 'Ativar tema escuro'}`}
    >
      {escuro ? <Sun className="size-4" /> : <Moon className="size-4" />}
    </button>
  )
}
