const ROTULOS = { PENDENTE: 'Aguardando aprovação', APROVADO: 'Aprovado', REJEITADO: 'Rejeitado' }

export function StatusModeracao({ material }) {
  const [agora, setAgora] = useState(Date.now)
  const data = material.decisaoEm || material.publicadoEm
  useEffect(() => {
    setAgora(Date.now())
    if (material.statusModeracao !== 'APROVADO') return
    const restante = Date.parse(data) + PRAZO_APROVACAO_RECENTE - Date.now()
    if (!Number.isFinite(restante) || restante <= 0 || restante > PRAZO_APROVACAO_RECENTE) return
    const timer = setTimeout(() => setAgora(Date.now()), restante)
    return () => clearTimeout(timer)
  }, [material.statusModeracao, data])
  if (!material.statusModeracao) return null
  if (material.statusModeracao === 'APROVADO' && !aprovacaoRecente(material, agora)) return null
  return <div className="space-y-2 text-sm">
    <p className={`font-semibold ${material.statusModeracao === 'REJEITADO' ? 'text-destructive' : 'text-secondary'}`}>
      {ROTULOS[material.statusModeracao] || material.statusModeracao}
    </p>
    {material.statusModeracao === 'REJEITADO' && material.justificativaRejeicao && <p className="whitespace-pre-wrap break-words text-muted-foreground">Motivo: {material.justificativaRejeicao}</p>}
  </div>
}
import { useEffect, useState } from 'react'
import { aprovacaoRecente, PRAZO_APROVACAO_RECENTE } from '../lib/moderacao.js'

