import { useEffect, useState } from 'react'

export function useTema() {
  const [tema, setTema] = useState(() => localStorage.getItem('banco-academico:tema') || 'claro')

  useEffect(() => {
    document.documentElement.classList.toggle('dark', tema === 'escuro')
    localStorage.setItem('banco-academico:tema', tema)
  }, [tema])

  return { tema, alternar: () => setTema(t => (t === 'escuro' ? 'claro' : 'escuro')) }
}
