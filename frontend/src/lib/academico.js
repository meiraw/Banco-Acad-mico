/** Aceita e-mails @unemat.br (e subdomínios) ou qualquer domínio contendo "unemat". */
export function isEmailInstitucional(email) {
  const valor = email.trim().toLowerCase()
  if (!/^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/.test(valor)) return false
  const dominio = valor.split('@')[1] ?? ''
  return dominio.includes('unemat')
}

export function avaliarSenha(senha) {
  const criterios = [
    { ok: senha.length >= 8, dica: 'pelo menos 8 caracteres' },
    { ok: /[0-9]/.test(senha), dica: 'um número' },
    { ok: /[^A-Za-z0-9]/.test(senha), dica: 'um caractere especial' },
    { ok: /[a-z]/.test(senha) && /[A-Z]/.test(senha), dica: 'letras maiúsculas e minúsculas' },
  ]
  const pontos = criterios.filter(c => c.ok).length
  const dicas = criterios.filter(c => !c.ok).map(c => c.dica)
  if (!senha) return { nivel: 0, rotulo: '', dicas }
  if (pontos <= 1) return { nivel: 1, rotulo: 'Fraca', dicas }
  if (pontos <= 3) return { nivel: 2, rotulo: 'Média', dicas }
  return { nivel: 3, rotulo: 'Forte', dicas }
}

/** Cores pastel cíclicas para identificar visualmente cada semestre no card/sidebar. */
const PALETA_SEMESTRE = ['#BFD7FF', '#C7E9C0', '#FFE0A3', '#FFC2C2', '#E3CCFF', '#B8EDF2', '#FFD1EC', '#F2E6A9']
export function corSemestre(indice) {
  return PALETA_SEMESTRE[indice % PALETA_SEMESTRE.length]
}

export function iniciais(nome) {
  return (nome || '')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(p => p[0]?.toUpperCase())
    .join('')
}

export function formatarData(data) {
  return new Date(data).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
}
