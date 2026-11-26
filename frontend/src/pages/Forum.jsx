import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Search, SlidersHorizontal, Plus, MessagesSquare, MessageCircle } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { AvatarAutor } from '../components/AvatarAutor.jsx'
import { PaginacaoForum } from '../components/PaginacaoForum.jsx'
import { api } from '../lib/api.js'

export function Forum() {
  const [params, setParams] = useSearchParams()
  const [pagina, setPagina] = useState(null)
  const [disciplinas, setDisciplinas] = useState([])
  const [erroDisciplinas, setErroDisciplinas] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [filtros, setFiltros] = useState(false)
  const [tentativa, setTentativa] = useState(0)
  const consulta = params.toString()
  useEffect(() => {
    let ativo = true
    setErroDisciplinas('')
    api.disciplinas().then(d => { if (ativo) setDisciplinas(d) }).catch(e => { if (ativo) setErroDisciplinas(e.message) })
    return () => { ativo = false }
  }, [tentativa])
  useEffect(() => {
    let ativo = true
    setCarregando(true); setErro(''); setPagina(null)
    const timer = setTimeout(() => {
      const p = new URLSearchParams(consulta)
      api.discussoes({ q: p.get('q') || '', disciplinaId: p.get('disciplinaId') || '', minhas: p.get('minhas') || 'false', ordem: p.get('ordem') || 'recentes', page: p.get('page') || '0', size: 20 })
        .then(d => { if (ativo) setPagina(d) }).catch(e => { if (ativo) setErro(e.message) })
        .finally(() => { if (ativo) setCarregando(false) })
    }, 300)
    return () => { ativo = false; clearTimeout(timer) }
  }, [consulta, tentativa])
  function alterar(chave, valor) {
    const next = new URLSearchParams(params)
    if (valor) next.set(chave, valor); else next.delete(chave)
    if (chave !== 'page') next.delete('page')
    setParams(next, { replace: chave === 'q' })
  }
  return <AppShell><div className="space-y-6">
    <header className="flex flex-wrap items-start justify-between gap-4">
      <div><h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Fórum Acadêmico</h1>
        <p className="mt-2 text-muted-foreground">Tire dúvidas, discuta tópicos das disciplinas e compartilhe conhecimento com a turma.</p></div>
      <Link to="/forum/nova" className="btn btn-primary"><Plus className="size-4" /> Nova discussão</Link>
    </header>
    <div>
      <div className="relative">
        <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
        <input aria-label="Pesquisar discussões" className="input-base h-12 rounded-full pl-12 pr-28" maxLength={200} placeholder="Buscar tópicos, dúvidas ou disciplinas..." value={params.get('q') || ''} onChange={e => alterar('q', e.target.value)} />
        <button type="button" aria-expanded={filtros} aria-controls="filtros-forum" onClick={() => setFiltros(v => !v)} className="btn btn-ghost absolute right-2 top-1/2 -translate-y-1/2 rounded-full text-primary"><SlidersHorizontal className="size-4" /> Filtros</button>
      </div>
      {filtros && <section id="filtros-forum" aria-label="Filtros do fórum" className="card-surface mt-3 grid gap-4 p-4 sm:grid-cols-2">
        <div><label htmlFor="forum-disciplina" className="label-mono mb-2 block">Disciplina</label>
          <select id="forum-disciplina" className="input-base" value={params.get('disciplinaId') || ''} onChange={e => alterar('disciplinaId', e.target.value)}>
            <option value="">Todas as disciplinas</option>{disciplinas.map(d => <option key={d.id} value={d.id}>{d.nome}</option>)}
          </select></div>
        <div><label htmlFor="forum-ordem" className="label-mono mb-2 block">Ordenar por</label>
          <select id="forum-ordem" className="input-base" value={params.get('ordem') || 'recentes'} onChange={e => alterar('ordem', e.target.value)}><option value="recentes">Mais recentes</option><option value="antigas">Mais antigas</option></select></div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={params.get('minhas') === 'true'} onChange={e => alterar('minhas', e.target.checked ? 'true' : '')} /> Minhas discussões</label>
        <button type="button" className="btn btn-ghost justify-self-start sm:justify-self-end" onClick={() => setParams({})}>Limpar filtros</button>
        {erroDisciplinas && <p role="alert" className="text-sm text-destructive sm:col-span-2">{erroDisciplinas} <button className="underline" onClick={() => setTentativa(v => v + 1)}>Tentar novamente</button></p>}
      </section>}
    </div>
    {carregando && <p role="status" className="text-muted-foreground">Carregando discussões...</p>}
    {erro && <div role="alert" className="card-surface p-5 text-destructive">{erro} <button className="underline" onClick={() => setTentativa(v => v + 1)}>Tentar novamente</button></div>}
    {!carregando && !erro && pagina?.content.length === 0 && <div className="card-surface flex flex-col items-center gap-3 px-5 py-12 text-center">
      <MessagesSquare className="size-12 text-secondary" aria-hidden="true" /><h2 className="text-xl font-semibold">{consulta ? 'Nenhuma discussão encontrada' : 'Ainda não há discussões'}</h2>
      <p className="text-muted-foreground">{consulta ? 'Experimente outra pesquisa ou limpe os filtros.' : 'Comece uma conversa e compartilhe conhecimento com a turma.'}</p>
      {consulta ? <button className="btn btn-outline" onClick={() => setParams({})}>Limpar filtros</button> : <Link to="/forum/nova" className="btn btn-primary">Nova discussão</Link>}
      {pagina.number > 0 && <button className="btn btn-outline" onClick={() => alterar('page', '0')}>Primeira página</button>}
    </div>}
    <div className="space-y-4">{pagina?.content.map(d => <Link key={d.id} to={`/forum/${d.id}`} className="card-surface block min-w-0 space-y-3 p-5 transition-colors hover:border-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary">
      <div className="flex flex-wrap items-center justify-between gap-2"><span className="chip on max-w-full break-words whitespace-normal">{d.disciplinaNome}</span><time dateTime={d.criadoEm} className="text-xs text-muted-foreground">{new Date(d.criadoEm).toLocaleString('pt-BR')}</time></div>
      <h2 className="break-words text-lg font-semibold text-primary">{d.titulo}</h2><p className="break-words text-sm text-muted-foreground">{d.previa}{d.previa.length === 240 ? '…' : ''}</p>
      <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground"><span className="flex min-w-0 items-center gap-2"><AvatarAutor discussaoId={d.id} autorNome={d.autorNome} className="size-8 shrink-0 text-xs" /><span className="break-words">{d.autorNome}</span></span><span className="flex items-center gap-2"><MessageCircle className="size-4" />{d.totalRespostas} {d.totalRespostas === 1 ? 'resposta' : 'respostas'}</span></div>
    </Link>)}</div>
    <PaginacaoForum pagina={pagina} onChange={p => alterar('page', String(p))} desabilitado={carregando} />
  </div></AppShell>
}
