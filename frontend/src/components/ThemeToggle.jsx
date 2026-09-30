import { Moon, Sun } from 'lucide-react'
import { useTema } from '../lib/useTema.js'

export function ThemeToggle() {
  const { tema, alternar } = useTema()
  const escuro = tema === 'escuro'
  return (
    <button
      type="button"
      onClick={alternar}
      className="btn btn-outline btn-icon"
      aria-label={escuro ? 'Ativar tema claro' : 'Ativar tema escuro'}
      title={escuro ? 'Tema claro' : 'Tema escuro'}
    >
      {escuro ? <Sun className="size-4" /> : <Moon className="size-4" />}
    </button>
  )
}
