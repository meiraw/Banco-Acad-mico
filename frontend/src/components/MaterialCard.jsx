import { Heart, ThumbsUp, ThumbsDown, Trash2 } from 'lucide-react'
import { api, categoria, rotuloSemestre } from '../lib/api.js'
import { corSemestre } from '../lib/academico.js'
import { useFavoritos } from '../lib/useFavoritos.js'
import { PreviaArquivo } from './PreviaArquivo.jsx'
import { Link } from 'react-router-dom'
import { Avatar } from './Avatar.jsx'
import { StatusModeracao } from './StatusModeracao.jsx'
import { rotuloTipoPublicacao } from '../lib/tipoPublicacao.js'
import { useState } from 'react'

export function MaterialCard({ material, disciplina, semestreIndice, semestre, cursos = [], onExcluir }) {
  const cat = categoria(material.tipoArquivo)
  const { isFavorito, alternar, disponivel, pendente, erro, recarregar } = useFavoritos()
  const [erroFavorito, setErroFavorito] = useState('')
  const [contagemAtualizada, setContagemAtualizada] = useState(null)
  const totais = contagemAtualizada?.material === material ? contagemAtualizada.dados : material
  const favorito = isFavorito(material.id)
  async function favoritar() {
    setErroFavorito('')
    try { await alternar(material.id) } catch (error) { setErroFavorito(error.message); return }
    try { setContagemAtualizada({ material, dados: await api.material(material.id) }) }
    catch { setErroFavorito('Favorito atualizado. Recarregue a página para atualizar os totais.') }
  }

  return (
    <article className="card-surface flex flex-col overflow-hidden transition-shadow hover:shadow-lg">
      <Link to={`/material/${material.id}`} aria-label={`Abrir publicação: ${material.titulo}`} className="block w-full text-left">
        <PreviaArquivo material={material} categoriaTipo={cat} />
      </Link>
      <div className="flex flex-1 flex-col gap-3 p-4">
        <StatusModeracao material={material} />
        <div className="flex items-start justify-between gap-2">
          {semestreIndice != null ? (
            <span className="label-mono w-fit rounded-md px-2 py-1 font-semibold text-neutral-900" style={{ backgroundColor: corSemestre(semestreIndice) }}>
              {semestre ? rotuloSemestre(semestre) : 'Semestre'}
            </span>
          ) : <span />}
          <button
            type="button"
            onClick={favoritar}
            disabled={!disponivel || pendente(material.id) || material.statusModeracao !== 'APROVADO'}
            aria-pressed={favorito}
            aria-label={favorito ? 'Remover dos favoritos' : 'Favoritar material'}
            title={material.statusModeracao !== 'APROVADO' ? 'Somente materiais aprovados podem ser favoritados.' : favorito ? 'Remover dos favoritos' : 'Favoritar material'}
            aria-busy={pendente(material.id)}
            className={`grid size-8 shrink-0 place-items-center rounded-full border transition-colors ${
              favorito ? 'border-destructive/40 bg-destructive/10 text-destructive' : 'border-border text-muted-foreground hover:border-destructive/40 hover:text-destructive'
            }`}
          >
            <Heart className={`size-4 ${favorito ? 'fill-current' : ''}`} />
          </button>
        </div>

        {semestre && <p className="label-mono text-muted-foreground">{cursos.find(c => c.id === semestre.cursoId)?.nome}</p>}

        <h3 className="line-clamp-2 text-lg font-semibold">
          <Link to={`/material/${material.id}`} className="text-left transition-colors hover:text-primary">
            {material.titulo}
          </Link>
        </h3>

        <div className="flex min-w-0 items-center gap-2">
          <Avatar nome={material.autorNome} className="size-7 text-xs" />
          <p className="min-w-0 break-words text-sm text-muted-foreground">
            Publicado por <span className="font-semibold text-foreground">{material.autorNome || 'Autor não informado'}</span>
          </p>
        </div>

        <span className="label-mono w-fit rounded-full border border-border bg-surface-container px-2.5 py-1 text-muted-foreground">
          {disciplina?.nome ?? 'Sem disciplina'}
        </span>

        <p className="line-clamp-2 text-sm text-muted-foreground">{material.descricao}</p>
        <p className="text-sm text-muted-foreground">Tipo de Publicação: {rotuloTipoPublicacao(material.tipoPublicacao)}</p>

        <div className="mt-auto flex flex-wrap items-center justify-between gap-3 border-t border-border pt-3">
          <div role="group" aria-label="Totais da publicação" className="flex items-center gap-4 text-sm text-muted-foreground tabular-nums">
            <span className="inline-flex items-center gap-1.5" title="Curtidas" aria-label={`${totais.totalCurtidas ?? '—'} curtidas`}>
              <ThumbsUp className="size-4" aria-hidden="true" /> {totais.totalCurtidas ?? '—'}
            </span>
            <span className="inline-flex items-center gap-1.5" title="Descurtidas" aria-label={`${totais.totalDeslikes ?? '—'} descurtidas`}>
              <ThumbsDown className="size-4" aria-hidden="true" /> {totais.totalDeslikes ?? '—'}
            </span>
            <span className="inline-flex items-center gap-1.5" title="Favoritos" aria-label={`${totais.totalFavoritos ?? '—'} favoritos`}>
              <Heart className="size-4" aria-hidden="true" /> {totais.totalFavoritos ?? '—'}
            </span>
          </div>
          {onExcluir && (
            <button type="button" onClick={() => onExcluir(material)} className="label-mono flex items-center gap-1 text-destructive transition-opacity hover:opacity-80">
              <Trash2 className="size-4" /> Excluir
            </button>
          )}
        </div>
        {(erroFavorito || erro) && <p role="alert" className="text-sm text-destructive">{erroFavorito || erro} <button type="button" className="underline" onClick={() => { setErroFavorito(''); void recarregar() }}>Tentar novamente</button></p>}
      </div>
    </article>
  )
}
