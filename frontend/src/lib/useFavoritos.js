import { useEffect, useState } from 'react'

const CHAVE = 'banco-academico:favoritos'

function ler() {
  try { return new Set(JSON.parse(localStorage.getItem(CHAVE)) || []) } catch { return new Set() }
}

export function useFavoritos() {
  const [ids, setIds] = useState(ler)

  useEffect(() => { localStorage.setItem(CHAVE, JSON.stringify([...ids])) }, [ids])

  return {
    isFavorito: id => ids.has(id),
    alternar: id => setIds(prev => {
      const novo = new Set(prev)
      novo.has(id) ? novo.delete(id) : novo.add(id)
      return novo
    }),
    lista: ids,
  }
}
