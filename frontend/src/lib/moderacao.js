export const PRAZO_APROVACAO_RECENTE = 4 * 24 * 60 * 60 * 1000

export function aprovacaoRecente(material, agora = Date.now()) {
  if (material.statusModeracao !== 'APROVADO') return false
  const data = Date.parse(material.decisaoEm || material.publicadoEm)
  const idade = agora - data
  return Number.isFinite(data) && idade >= 0 && idade < PRAZO_APROVACAO_RECENTE
}
