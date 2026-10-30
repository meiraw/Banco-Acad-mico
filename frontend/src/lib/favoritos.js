const PREFIXO = 'banco-academico:favoritos:'

export function chaveFavoritos(usuarioId) {
  return usuarioId ? `${PREFIXO}${usuarioId}` : null
}

export function lerFavoritos(usuarioId) {
  const chave = chaveFavoritos(usuarioId)
  if (!chave) return '[]'
  try { return localStorage.getItem(chave) || '[]' } catch { return '[]' }
}

export function idsFavoritos(serializado) {
  try {
    const ids = JSON.parse(serializado)
    return new Set(Array.isArray(ids) ? ids.filter(id => typeof id === 'string') : [])
  } catch { return new Set() }
}

export function alternarFavorito(usuarioId, materialId) {
  const chave = chaveFavoritos(usuarioId)
  if (!chave) return
  const ids = idsFavoritos(lerFavoritos(usuarioId))
  ids.has(materialId) ? ids.delete(materialId) : ids.add(materialId)
  localStorage.setItem(chave, JSON.stringify([...ids]))
  window.dispatchEvent(new CustomEvent('favoritos-atualizados', { detail: { chave } }))
}
