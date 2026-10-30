export function chaveTema(conta) {
  return conta ? `banco-academico:tema:${conta}` : null
}

export function lerTema(conta) {
  const chave = chaveTema(conta)
  if (!chave) return 'claro'
  try { return localStorage.getItem(chave) === 'escuro' ? 'escuro' : 'claro' } catch { return 'claro' }
}

export function alternarTema(conta) {
  const chave = chaveTema(conta)
  if (!chave) return
  const tema = lerTema(conta) === 'escuro' ? 'claro' : 'escuro'
  localStorage.setItem(chave, tema)
  window.dispatchEvent(new CustomEvent('tema-atualizado', { detail: { chave } }))
}
