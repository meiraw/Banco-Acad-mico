import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { AppShell } from '../components/AppShell.jsx'
import { Avatar } from '../components/Avatar.jsx'
import { FormularioDiscussao } from '../components/FormularioDiscussao.jsx'
import { RespostaForum } from '../components/RespostaForum.jsx'
import { PaginacaoForum } from '../components/PaginacaoForum.jsx'
import { useSessao } from '../lib/sessao.jsx'
import { api } from '../lib/api.js'

export function Discussao() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { perfil } = useSessao()
  const [discussao, setDiscussao] = useState(null)
  const [respostas, setRespostas] = useState(null)
  const [pagina, setPagina] = useState(0)
  const [revisao, setRevisao] = useState(0)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [erroAcao, setErroAcao] = useState('')
  const [texto, setTexto] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [editando, setEditando] = useState(false)
  const [ocupado, setOcupado] = useState(false)
  const envio = useRef(false)
  const ultimaPagina = useRef(false)
  useEffect(() => {
    setDiscussao(null); setRespostas(null); setPagina(0); setTexto(''); setEditando(false); setMensagem(''); setErroAcao(''); ultimaPagina.current = false
  }, [id])
  useEffect(() => {
    let ativo = true
    setCarregando(true); setErro('')
    async function carregar() {
      try {
        const d = await api.discussao(id)
        const alvo = ultimaPagina.current ? Math.max(0, Math.ceil(d.totalRespostas / 20) - 1) : Math.min(pagina, Math.max(0, Math.ceil(d.totalRespostas / 20) - 1))
        const r = await api.respostasForum(id, alvo)
        if (ativo) { setDiscussao(d); setRespostas(r); ultimaPagina.current = false; if (alvo !== pagina) setPagina(alvo) }
      } catch (e) { if (ativo) { setErro(e.message); setDiscussao(null); setRespostas(null) } }
      finally { if (ativo) setCarregando(false) }
    }
    carregar()
    return () => { ativo = false }
  }, [id, pagina, revisao])
  const atualizar = () => { setEditando(false); setRevisao(v => v + 1) }
  const propria = !!discussao?.autorId && perfil?.id === discussao.autorId
  async function editar(dados) {
    const d = await api.editarDiscussao(id, dados)
    setDiscussao(d); setEditando(false); setMensagem('Discussão atualizada.'); setErroAcao('')
  }
  async function excluir() {
    if (envio.current || !window.confirm('Excluir esta discussão? Ela e suas respostas deixarão de aparecer no fórum.')) return
    envio.current = true; setOcupado(true); setErroAcao('')
    try { await api.excluirDiscussao(id); navigate('/forum', { replace: true }) }
    catch (e) { setErroAcao(e.message) }
    finally { envio.current = false; setOcupado(false) }
  }
  async function responder(e) {
    e.preventDefault()
    if (envio.current) return
    if (!texto.trim()) { setErroAcao('Escreva uma resposta.'); return }
    envio.current = true; setOcupado(true); setErroAcao(''); setMensagem('')
    try {
      await api.responderForum(id, texto.trim())
      setTexto(''); setMensagem('Resposta publicada.'); ultimaPagina.current = true; setRevisao(v => v + 1)
    } catch (e) { setErroAcao(e.message) }
    finally { envio.current = false; setOcupado(false) }
  }
  return <AppShell><div className="mx-auto max-w-4xl space-y-6">
    <Link to="/forum" className="text-sm font-semibold text-secondary hover:underline">Fórum Acadêmico</Link>
    {carregando && <p role="status" className="text-muted-foreground">Carregando discussão...</p>}
    {erro && <div role="alert" className="card-surface p-5 text-destructive">{erro} <button className="underline" onClick={atualizar}>Tentar novamente</button></div>}
    {mensagem && <p role="status" className="text-sm text-secondary">{mensagem}</p>}
    {discussao && <>
      {editando ? <div className="space-y-3"><h1 className="text-2xl font-bold">Editar discussão</h1><FormularioDiscussao key={`${discussao.id}-${discussao.versao}`} inicial={discussao} onSalvar={editar} onCancelar={() => setEditando(false)} /><button className="text-sm text-secondary underline" onClick={atualizar}>Recarregar versão atual</button></div> : <article className="card-surface min-w-0 space-y-5 p-5 sm:p-8">
        <header className="space-y-4"><span className="chip on max-w-full break-words whitespace-normal">{discussao.disciplinaNome}</span><h1 className="break-words text-2xl font-bold sm:text-3xl">{discussao.titulo}</h1>
          <div className="flex flex-wrap items-center justify-between gap-4"><div className="flex min-w-0 items-center gap-3"><Avatar nome={discussao.autorNome} className="size-10 shrink-0 text-sm" /><div className="min-w-0"><p className="break-words font-semibold">{discussao.autorNome}</p><time dateTime={discussao.criadoEm} className="text-xs text-muted-foreground">{new Date(discussao.criadoEm).toLocaleString('pt-BR')}</time></div></div>
            <div className="flex flex-wrap gap-2">{propria && <button type="button" className="btn btn-outline" disabled={ocupado || carregando} onClick={() => setEditando(true)}>Editar</button>}{(propria || perfil?.perfil === 'ADMIN') && <button type="button" className="btn btn-outline text-destructive" disabled={ocupado || carregando} onClick={excluir}>Excluir</button>}</div>
          </div></header>
        <p className="whitespace-pre-wrap break-words leading-relaxed">{discussao.conteudo}</p>
        {discussao.atualizadoEm !== discussao.criadoEm && <p className="text-xs text-muted-foreground">Editada em {new Date(discussao.atualizadoEm).toLocaleString('pt-BR')}</p>}
      </article>}
      <section aria-labelledby="respostas-forum" className="space-y-4"><h2 id="respostas-forum" className="text-xl font-semibold">{discussao.totalRespostas} {discussao.totalRespostas === 1 ? 'resposta' : 'respostas'}</h2>
        {!carregando && !respostas?.content.length && <p className="text-muted-foreground">Ainda não há respostas. Compartilhe o que você sabe.</p>}
        {respostas?.content.map(r => <RespostaForum key={`${r.id}-${r.versao}`} resposta={r} onAtualizar={atualizar} />)}
        <PaginacaoForum pagina={respostas} onChange={setPagina} desabilitado={carregando || ocupado} />
      </section>
      <form onSubmit={responder} className="card-surface space-y-3 p-5"><label htmlFor="nova-resposta" className="block font-semibold">Sua resposta</label>
        <textarea id="nova-resposta" className="input-base resize-y" required maxLength={5000} rows={5} value={texto} onChange={e => setTexto(e.target.value)} disabled={ocupado} placeholder="Compartilhe sua explicação com a turma..." />
        <div className="flex flex-wrap items-center justify-between gap-3"><span className="text-xs text-muted-foreground">{texto.length}/5.000</span><button className="btn btn-primary" disabled={ocupado || carregando}>{ocupado ? 'Enviando...' : 'Publicar resposta'}</button></div>
      </form>
    </>}
    {erroAcao && <p role="alert" className="text-sm text-destructive">{erroAcao}</p>}
  </div></AppShell>
}
