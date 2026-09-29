import { FileText, Presentation, ImageIcon, FileQuestion, Eye, Heart, Trash2 } from 'lucide-react'
import { categoria, tamanho } from '../lib/api.js'
import { corSemestre } from '../lib/academico.js'
import { useFavoritos } from '../lib/useFavoritos.js'
import { PreviaArquivo } from './PreviaArquivo.jsx'

const ICONES = { PDF: FileText, Slides: Presentation, Imagem: ImageIcon, Outro: FileQuestion }

export function MaterialCard({ material, disciplina, semestreIndice, onVisualizar, onExcluir }) {
  const cat = categoria(material.tipoArquivo)
  const Icone = ICONES[cat]
  const { isFavorito, alternar } = useFavoritos()
  const favorito = isFavorito(material.id)

  return (
    <article className="card-surface flex flex-col overflow-hidden transition-shadow hover:shadow-lg">
      <button type="button" onClick={() => onVisualizar(material)} aria-label={`Pré-visualizar ${material.titulo}`} className="block w-full text-left">
        <PreviaArquivo material={material} categoriaTipo={cat} />
      </button>
      <div className="flex flex-1 flex-col gap-3 p-4">
        <div className="flex items-start justify-between gap-2">
          {semestreIndice != null ? (
            <span className="label-mono w-fit rounded-md px-2 py-1 font-semibold text-neutral-900" style={{ backgroundColor: corSemestre(semestreIndice) }}>
              {disciplina?.nome ? 'Semestre' : '—'}
            </span>
          ) : <span />}
          <button
            type="button"
            onClick={() => alternar(material.id)}
            aria-pressed={favorito}
            aria-label={favorito ? 'Remover dos favoritos' : 'Favoritar material'}
            title={favorito ? 'Remover dos favoritos' : 'Favoritar material'}
            className={`grid size-8 shrink-0 place-items-center rounded-full border transition-colors ${
              favorito ? 'border-destructive/40 bg-destructive/10 text-destructive' : 'border-border text-muted-foreground hover:border-destructive/40 hover:text-destructive'
            }`}
          >
            <Heart className={`size-4 ${favorito ? 'fill-current' : ''}`} />
          </button>
        </div>

        <h3 className="line-clamp-2 text-lg font-semibold">
          <button type="button" onClick={() => onVisualizar(material)} className="text-left transition-colors hover:text-primary">
            {material.titulo}
          </button>
        </h3>

        <span className="label-mono w-fit rounded-full border border-border bg-surface-container px-2.5 py-1 text-muted-foreground">
          {disciplina?.nome ?? 'Sem disciplina'}
        </span>

        <p className="line-clamp-2 text-sm text-muted-foreground">{material.descricao}</p>

        <div className="mt-auto flex items-center justify-between gap-2 border-t border-border pt-3">
          <span className="label-mono flex items-center gap-1.5 text-muted-foreground">
            <Icone className="size-4" /> {cat} · {tamanho(material.tamanhoArquivo)}
          </span>
          <div className="flex items-center gap-3">
            <button type="button" onClick={() => onVisualizar(material)} className="label-mono flex items-center gap-1 text-secondary transition-opacity hover:opacity-80">
              <Eye className="size-4" /> Visualizar
            </button>
            {onExcluir && (
              <button type="button" onClick={() => onExcluir(material)} className="label-mono flex items-center gap-1 text-destructive transition-opacity hover:opacity-80">
                <Trash2 className="size-4" /> Excluir
              </button>
            )}
          </div>
        </div>
      </div>
    </article>
  )
}
