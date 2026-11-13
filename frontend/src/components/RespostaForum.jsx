import { useRef, useState } from 'react'
import { Avatar } from './Avatar.jsx'
import { api } from '../lib/api.js'
import { useSessao } from '../lib/sessao.jsx'

export function RespostaForum({ resposta, onAtualizar }) {
  const { perfil } = useSessao()
  const propria = !!resposta.autorId && perfil?.id === resposta.autorId
  const [editando, setEditando] = useState(false)
  const [texto, setTexto] = useState(resposta.conteudo)
  const [erro, setErro] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const envio = useRef(false)
  async function executar(operacao) {
    if (envio.current) return
    envio.current = true; setOcupado(true); setErro('')
    try { await operacao(); setEditando(false); onAtualizar() }
    catch (e) { setErro(e.message) }
    finally { envio.current = false; setOcupado(false) }
  }
  function excluir() {
    if (window.confirm('Excluir esta resposta? Ela deixará de aparecer na discussão.'))
      executar(() => api.excluirRespostaForum(resposta.discussaoId, resposta.id))
  }
  function salvar(e) {
    e.preventDefault()
    if (!texto.trim()) { setErro('Escreva uma resposta.'); return }
    executar(() => api.editarRespostaForum(resposta.discussaoId, resposta.id, { conteudo: texto.trim(), versao: resposta.versao }))
  }
  return <article className="card-surface min-w-0 space-y-3 p-5">
    <header className="flex flex-wrap items-start justify-between gap-3">
      <div className="flex min-w-0 items-center gap-3"><Avatar nome={resposta.autorNome} className="size-9 shrink-0 text-sm" /><div className="min-w-0"><p className="break-words font-semibold">{resposta.autorNome}</p><time className="text-xs text-muted-foreground" dateTime={resposta.criadoEm}>{new Date(resposta.criadoEm).toLocaleString('pt-BR')}</time></div></div>
      <div className="flex flex-wrap gap-2">{propria && !editando && <button type="button" className="btn btn-ghost" disabled={ocupado} onClick={() => { setTexto(resposta.conteudo); setErro(''); setEditando(true) }}>Editar</button>}
        {(propria || perfil?.perfil === 'ADMIN') && <button type="button" className="btn btn-ghost text-destructive" disabled={ocupado} onClick={excluir}>Excluir</button>}</div>
    </header>
    {editando ? <form onSubmit={salvar} className="space-y-3">
      <label className="label-mono block" htmlFor={`editar-resposta-${resposta.id}`}>Editar resposta</label>
      <textarea id={`editar-resposta-${resposta.id}`} required maxLength={5000} rows={5} value={texto} disabled={ocupado} onChange={e => setTexto(e.target.value)} className="input-base resize-y" />
      <div className="flex flex-wrap justify-end gap-2"><button type="button" className="btn btn-outline" disabled={ocupado} onClick={() => setEditando(false)}>Cancelar</button><button className="btn btn-primary" disabled={ocupado}>{ocupado ? 'Salvando...' : 'Salvar'}</button></div>
    </form> : <p className="whitespace-pre-wrap break-words text-sm leading-relaxed">{resposta.conteudo}</p>}
    {resposta.atualizadoEm !== resposta.criadoEm && <p className="text-xs text-muted-foreground">Editada em {new Date(resposta.atualizadoEm).toLocaleString('pt-BR')}</p>}
    {erro && <p role="alert" className="text-sm text-destructive">{erro} <button type="button" className="underline" onClick={onAtualizar}>Atualizar discussão</button></p>}
  </article>
}
