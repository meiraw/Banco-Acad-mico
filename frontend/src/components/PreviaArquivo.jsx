import { FileText, ImageIcon, Presentation, FileQuestion } from 'lucide-react'
import { api } from '../lib/api.js'
import { PreviaPdf } from './PreviaPdf.jsx'

export function PreviaArquivo({ material, categoriaTipo }) {
  const url = api.urlArquivo(material.id)
  const Icone = categoriaTipo === 'Imagem' ? ImageIcon : categoriaTipo === 'Slides' ? Presentation : categoriaTipo === 'PDF' ? FileText : FileQuestion

  return (
    <div className="relative h-32 w-full overflow-hidden border-b border-border bg-surface-container">
      {material.statusModeracao && material.statusModeracao !== 'APROVADO' ? (
        <div className="flex size-full items-center justify-center px-4 text-center text-sm text-muted-foreground">Abra a publicação para visualizar o arquivo.</div>
      ) : !material.arquivoValidado ? (
        <div className="flex size-full items-center justify-center text-sm text-muted-foreground">Disponível para download</div>
      ) : categoriaTipo === 'PDF' ? (
        <PreviaPdf url={url} titulo={material.titulo} miniatura />
      ) : categoriaTipo === 'Imagem' ? (
        <img src={url} alt={`Prévia de ${material.titulo}`} loading="lazy" className="size-full object-cover" />
      ) : (
        <div className="flex size-full flex-col items-center justify-center gap-1 text-muted-foreground">
          <Icone className="size-7" />
          <span className="label-mono">Sem prévia disponível</span>
        </div>
      )}
    </div>
  )
}
