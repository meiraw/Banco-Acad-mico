import { rotuloTipoPublicacao, permiteReferencia } from '../lib/tipoPublicacao.js'

export function DadosPublicacao({ material }) {
  return <div className="space-y-2 text-sm text-muted-foreground">
    <p>Tipo de Publicação: <span className="font-semibold text-foreground">{rotuloTipoPublicacao(material.tipoPublicacao)}</span></p>
    {permiteReferencia(material.tipoPublicacao) && material.referenciaBibliografica && <div>
      <p className="font-semibold text-foreground">Referência Bibliográfica do {rotuloTipoPublicacao(material.tipoPublicacao)}</p>
      <p className="whitespace-pre-wrap break-words">{material.referenciaBibliografica}</p>
    </div>}
  </div>
}
