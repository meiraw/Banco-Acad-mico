import { X, Download, Trash2 } from 'lucide-react'
import { api, categoria, tamanho } from '../lib/api.js'

export function VisualizadorMaterial({ material, disciplina, onClose, onExcluir }) {
  if (!material) return null
  const cat = categoria(material.tipoArquivo)
  const url = api.urlArquivo(material.id)

  return (
    <div className="fixed inset-0 z-40 grid place-items-center bg-foreground/40 p-4" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <div role="dialog" aria-modal="true" aria-label={material.titulo} className="card-surface w-full max-w-3xl max-h-[92vh] overflow-y-auto p-6">
        <header className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold">{material.titulo}</h2>
            <p className="label-mono mt-1 text-muted-foreground">
              {disciplina?.nome ?? 'Sem disciplina'} · {material.nomeArquivo} · {tamanho(material.tamanhoArquivo)}
            </p>
          </div>
          <button onClick={onClose} aria-label="Fechar" className="btn btn-ghost btn-icon shrink-0"><X className="size-5" /></button>
        </header>

        <p className="mt-4 text-sm text-muted-foreground">{material.descricao}</p>

        <div className="mt-4 overflow-hidden rounded-lg border border-border bg-surface-container">
          {cat === 'PDF' && <iframe src={url} title={material.titulo} className="h-[60vh] w-full" />}
          {cat === 'Imagem' && <img src={url} alt={material.titulo} className="mx-auto max-h-[60vh] object-contain" />}
          {(cat === 'Slides' || cat === 'Outro') && (
            <p className="p-8 text-center text-muted-foreground">Este formato não tem prévia. Baixe o arquivo para abrir.</p>
          )}
        </div>

        <footer className="mt-6 flex justify-end gap-3">
          {onExcluir && (
            <button onClick={() => confirm('Excluir este material?') && onExcluir(material)} className="btn btn-outline mr-auto text-destructive border-destructive/40 hover:bg-destructive/10">
              <Trash2 className="size-4" /> Excluir
            </button>
          )}
          <a href={url} download={material.nomeArquivo} className="btn btn-primary"><Download className="size-4" /> Baixar arquivo</a>
        </footer>
      </div>
    </div>
  )
}
