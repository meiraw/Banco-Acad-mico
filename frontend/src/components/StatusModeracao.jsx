const ROTULOS = { PENDENTE: 'Aguardando aprovação', APROVADO: 'Aprovado', REJEITADO: 'Rejeitado' }

export function StatusModeracao({ material }) {
  if (!material.statusModeracao) return null
  return <div className="space-y-2 text-sm">
    <p className={`font-semibold ${material.statusModeracao === 'REJEITADO' ? 'text-destructive' : 'text-secondary'}`}>
      {ROTULOS[material.statusModeracao] || material.statusModeracao}
    </p>
    {material.statusModeracao === 'REJEITADO' && material.justificativaRejeicao && <p className="whitespace-pre-wrap break-words text-muted-foreground">Motivo: {material.justificativaRejeicao}</p>}
  </div>
}
