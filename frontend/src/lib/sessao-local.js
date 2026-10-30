export function claims(token) {
  const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
  return JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(payload), c => c.charCodeAt(0))))
}

export function carregarSessao() {
  try {
    const token = sessionStorage.getItem('banco-academico:token')
    if (!token) return null
    const info = claims(token)
    if (!info.sub || !info.exp || info.exp * 1000 <= Date.now()) return null
    let dados = null
    try { dados = JSON.parse(sessionStorage.getItem('banco-academico:sessao')) } catch { /* Reconsulta o perfil. */ }
    // Não reutiliza ID, nome ou foto de uma conta diferente da sessão ativa.
    return { ...(dados?.email === info.sub ? dados : {}), email: info.sub, perfil: info.perfil, token }
  } catch { return null }
}
