import { FileText, ImageIcon, Presentation, FileQuestion } from 'lucide-react'
import { api } from '../lib/api.js'

export function PreviaArquivo({ material, categoriaTipo }) {
  const url = api.urlArquivo(material.id)
  const Icone = categoriaTipo === 'Imagem' ? ImageIcon : categoriaTipo === 'Slides' ? Presentation : categoriaTipo === 'PDF' ? FileText : FileQuestion

  return (
    <div className="relative h-32 w-full overflow-hidden border-b border-border bg-surface-container">
      {categoriaTipo === 'PDF' ? (
        <iframe
          src={`${url}#toolbar=0&navpanes=0&view=FitH`}
          title={`Prévia de ${material.titulo}`}
          tabIndex={-1}
          aria-hidden
          className="pointer-events-none h-[420px] w-full origin-top scale-[0.55]"
        />
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
