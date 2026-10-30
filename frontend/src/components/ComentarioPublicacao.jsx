import { useEffect, useRef, useState } from 'react'
import { api } from '../lib/api.js'
import { Avatar } from './Avatar.jsx'
import { useSessao } from '../lib/sessao.jsx'

const data = valor => new Date(valor).toLocaleString('pt-BR')
const ordenarRespostas = lista => [...lista].sort((a, b) => new Date(a.criadoEm) - new Date(b.criadoEm) || a.id.localeCompare(b.id))

export function ComentarioPublicacao({ materialId, comentario }) {
  const { perfil } = useSessao()
  const podeResponder = !!perfil?.id && !!comentario.autorId && perfil.id !== comentario.autorId
  const [aberto, setAberto] = useState(false)
  const [respondendo, setRespondendo] = useState(false)
  const [alvoResposta, setAlvoResposta] = useState(null)
  const podeResponderAlvo = alvoResposta ? !!perfil?.id && !!alvoResposta.autorId && perfil.id !== alvoResposta.autorId : podeResponder
  const [respostas, setRespostas] = useState([])
  const [total, setTotal] = useState(comentario.totalRespostas || 0)
  const [pagina, setPagina] = useState(0)
  const [temMais, setTemMais] = useState(false)
  const [carregando, setCarregando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [carregado, setCarregado] = useState(false)
  const [texto, setTexto] = useState('')
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const envio = useRef(false)
  const consulta = useRef(false)

  useEffect(() => { setTotal(comentario.totalRespostas || 0) }, [comentario.totalRespostas])

  async function carregar(proxima = 0) {
    if (consulta.current || envio.current) return
    consulta.current = true; setCarregando(true); setErro('')
    try {
      const lista = await api.respostas(materialId, comentario.id, proxima)
      setRespostas(anteriores => ordenarRespostas(proxima === 0 ? lista : [...anteriores, ...lista.filter(r => !anteriores.some(a => a.id === r.id))]))
      setPagina(proxima); setTemMais(lista.length === 50); setCarregado(true)
    } catch (e) { setErro(e.message) }
    finally { consulta.current = false; setCarregando(false) }
  }

  function abrir(paraResponder = false) {
    if (paraResponder && !podeResponder) return
    setAberto(true)
    if (paraResponder) { setAlvoResposta(null); setRespondendo(true); setTexto(''); setMensagem('') }
    if (!carregado) carregar()
  }

  function responderOutraResposta(resposta) {
    if (!perfil?.id || !resposta.autorId || perfil.id === resposta.autorId || envio.current) return
    setAlvoResposta(resposta); setRespondendo(true); setTexto(''); setMensagem('')
  }

  async function responder(event) {
    event.preventDefault()
    if (!podeResponderAlvo || !texto.trim() || envio.current || consulta.current) return
    envio.current = true; setEnviando(true); setErro(''); setMensagem('')
    try {
      const resposta = await api.responder(materialId, comentario.id, texto.trim(), alvoResposta?.id)
      setRespostas(anteriores => ordenarRespostas([...anteriores, resposta])); setTotal(t => t + 1)
      setTexto(''); setRespondendo(false); setMensagem('Resposta publicada.')
    } catch (e) { setErro(e.message) }
    finally { envio.current = false; setEnviando(false) }
  }

  return <article className="border-t border-border pt-5">
    <div className="flex gap-3">
      <Avatar nome={comentario.autorNome} className="size-9" />
      <div className="min-w-0 flex-1">
        <p className="font-medium">{comentario.autorNome}</p>
        <time dateTime={comentario.criadoEm} className="text-xs text-muted-foreground">{data(comentario.criadoEm)}</time>
        <p className="mt-2 whitespace-pre-wrap break-words text-sm">{comentario.texto}</p>
        <div className="mt-3 flex flex-wrap gap-4 text-sm text-secondary">
          {podeResponder && <button type="button" onClick={() => abrir(true)}>Responder</button>}
          {total > 0 && <button type="button" aria-expanded={aberto} aria-controls={`respostas-${comentario.id}`} onClick={() => aberto ? setAberto(false) : abrir()}>
            {aberto ? 'Ocultar respostas' : `Ver respostas (${total})`}
          </button>}
        </div>

        {aberto && <div id={`respostas-${comentario.id}`} role="region" aria-label={`Respostas ao comentário de ${comentario.autorNome}`} tabIndex={0}
          className="mt-4 max-h-[60vh] space-y-4 overflow-y-auto overscroll-contain border-l-2 border-border pl-3 pr-3 focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary sm:pl-5">
          {respondendo && podeResponderAlvo && <form key={alvoResposta?.id || comentario.id} onSubmit={responder} className="space-y-3">
            <label htmlFor={`resposta-${comentario.id}`} className="block text-sm font-medium">Respondendo a {alvoResposta?.autorNome || comentario.autorNome}</label>
            {alvoResposta && <blockquote className="border-l-2 border-border pl-3 text-sm text-muted-foreground"><p className="line-clamp-3 break-words">{alvoResposta.texto}</p></blockquote>}
            <textarea id={`resposta-${comentario.id}`} autoFocus required maxLength={2000} rows={3} value={texto} onChange={e => setTexto(e.target.value)} disabled={enviando}
              className="input-base h-auto resize-y py-2" placeholder="Escreva sua resposta" />
            <div className="flex flex-wrap items-center gap-3"><span className="mr-auto text-xs text-muted-foreground">{texto.length}/2000</span>
              <button type="button" disabled={enviando} onClick={() => { setRespondendo(false); setTexto('') }} className="btn btn-ghost">Cancelar</button>
              <button type="submit" disabled={enviando || carregando || !texto.trim()} className="btn btn-primary">{enviando ? 'Publicando…' : 'Publicar resposta'}</button>
            </div>
          </form>}
          {mensagem && <p role="status" className="text-sm text-secondary">{mensagem}</p>}
          {erro && <p role="alert" className="text-sm text-destructive">{erro} <button type="button" disabled={carregando || enviando} onClick={() => carregar(0)} className="underline">Recarregar respostas</button></p>}
          {respostas.map(r => <article key={r.id} className="flex gap-2">
            <Avatar nome={r.autorNome} className="size-8 text-xs" />
            <div className="min-w-0"><p className="text-sm font-semibold">{r.autorNome}</p><time dateTime={r.criadoEm} className="text-xs text-muted-foreground">{data(r.criadoEm)}</time>
              {r.respondendoA && <p className="mt-1 text-xs text-secondary">Em resposta a {r.respondendoA}</p>}
              <p className="mt-1 whitespace-pre-wrap break-words text-sm">{r.texto}</p>
              {!!perfil?.id && !!r.autorId && perfil.id !== r.autorId && <button type="button" disabled={enviando} onClick={() => responderOutraResposta(r)} className="mt-2 text-sm text-secondary">Responder</button>}
            </div>
          </article>)}
          {carregando && <p role="status" className="text-sm text-muted-foreground">Carregando respostas…</p>}
          {temMais && <button type="button" disabled={carregando || enviando} onClick={() => carregar(pagina + 1)} className="btn btn-outline">Ver mais respostas</button>}
        </div>}
      </div>
    </div>
  </article>
}
