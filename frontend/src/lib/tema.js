export function chaveTema(conta) {
  return conta ? `banco-academico:tema:${conta}` : null
}

export function lerTema(conta) {
  const chave = chaveTema(conta)
  if (!chave) return 'claro'
  try {
    const valor = localStorage.getItem(chave)
    return ['claro', 'escuro', 'automatico'].includes(valor) ? valor : 'claro'
  } catch { return 'claro' }
}

export function resolverTema(preferencia, prefereEscuro) {
  return preferencia === 'automatico' ? (prefereEscuro ? 'escuro' : 'claro') : preferencia
}

export function assinarTemaSistema(media, notificar) {
  if (!media) return () => {}
  if (media.addEventListener) {
    media.addEventListener('change', notificar)
    return () => media.removeEventListener('change', notificar)
  }
  media.addListener(notificar)
  return () => media.removeListener(notificar)
}

export function definirTema(conta, tema) {
  const chave = chaveTema(conta)
  if (!chave || !['claro', 'escuro', 'automatico'].includes(tema)) return false
  try { localStorage.setItem(chave, tema) } catch { return false }
  window.dispatchEvent(new CustomEvent('tema-atualizado', { detail: { chave } }))
  return true
}

export function alternarTema(conta, temaAplicado) {
  const atual = temaAplicado || resolverTema(lerTema(conta), window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false)
  return definirTema(conta, atual === 'escuro' ? 'claro' : 'escuro')
}
