import { useCallback, useMemo, useSyncExternalStore } from 'react'
import { useSessao } from './sessao.jsx'
import { alternarFavorito, chaveFavoritos, idsFavoritos, lerFavoritos } from './favoritos.js'

export function useFavoritos() {
  const { perfil } = useSessao()
  const usuarioId = perfil?.id
  const assinar = useCallback(notificar => {
    const chave = chaveFavoritos(usuarioId)
    const atualizar = event => {
      if (chave && (event.key === null || event.key === chave || event.detail?.chave === chave)) notificar()
    }
    window.addEventListener('favoritos-atualizados', atualizar)
    window.addEventListener('storage', atualizar)
    return () => {
      window.removeEventListener('favoritos-atualizados', atualizar)
      window.removeEventListener('storage', atualizar)
    }
  }, [usuarioId])
  const ler = useCallback(() => lerFavoritos(usuarioId), [usuarioId])
  const serializado = useSyncExternalStore(assinar, ler)
  const ids = useMemo(() => idsFavoritos(serializado), [serializado])

  return {
    isFavorito: id => ids.has(id),
    alternar: id => alternarFavorito(usuarioId, id),
    disponivel: !!usuarioId,
    lista: ids,
  }
}
