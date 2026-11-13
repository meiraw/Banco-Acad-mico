import { useEffect, useRef, useState } from 'react'
import { api } from '../lib/api.js'

export function FormularioDiscussao({ inicial, onSalvar, onCancelar }) {
  const [titulo, setTitulo] = useState(inicial?.titulo || '')
  const [disciplinaId, setDisciplinaId] = useState(inicial?.disciplinaId || '')
  const [conteudo, setConteudo] = useState(inicial?.conteudo || '')
  const [disciplinas, setDisciplinas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erroDisciplinas, setErroDisciplinas] = useState('')
  const [tentativa, setTentativa] = useState(0)
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  const envio = useRef(false)
  useEffect(() => {
    let ativo = true
    setCarregando(true); setErroDisciplinas('')
    api.disciplinas().then(d => { if (ativo) setDisciplinas(d) })
      .catch(e => { if (ativo) setErroDisciplinas(e.message) })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [tentativa])
  async function enviar(e) {
    e.preventDefault()
    if (envio.current) return
    if (!titulo.trim() || !conteudo.trim() || !disciplinaId) { setErro('Preencha título, disciplina e conteúdo.'); return }
    envio.current = true; setEnviando(true); setErro('')
    try { await onSalvar({ titulo: titulo.trim(), disciplinaId, conteudo: conteudo.trim(), ...(inicial ? { versao: inicial.versao } : {}) }) }
    catch (e) { setErro(e.message) }
    finally { envio.current = false; setEnviando(false) }
  }
  return <form onSubmit={enviar} className="card-surface space-y-5 p-5 sm:p-6" aria-label={inicial ? 'Editar discussão' : 'Nova discussão'}>
    <div>
      <label htmlFor="discussao-titulo" className="label-mono mb-2 block">Título *</label>
      <input id="discussao-titulo" className="input-base" required maxLength={200} value={titulo} onChange={e => setTitulo(e.target.value)} disabled={enviando} />
    </div>
    <div>
      <label htmlFor="discussao-disciplina" className="label-mono mb-2 block">Disciplina *</label>
      <select id="discussao-disciplina" className="input-base" required value={disciplinaId} onChange={e => setDisciplinaId(e.target.value)} disabled={carregando || enviando || !!erroDisciplinas}>
        <option value="">{carregando ? 'Carregando disciplinas...' : 'Selecione uma disciplina'}</option>
        {disciplinas.map(d => <option key={d.id} value={d.id}>{d.nome}</option>)}
      </select>
      {erroDisciplinas && <div role="alert" className="mt-2 text-sm text-destructive">{erroDisciplinas} <button type="button" className="underline" onClick={() => setTentativa(t => t + 1)}>Tentar novamente</button></div>}
      {!carregando && !erroDisciplinas && !disciplinas.length && <p className="mt-2 text-sm text-muted-foreground">Não há disciplinas disponíveis. Solicite o cadastro à administração.</p>}
    </div>
    <div>
      <label htmlFor="discussao-conteudo" className="label-mono mb-2 block">Conteúdo *</label>
      <textarea id="discussao-conteudo" className="input-base min-h-48 resize-y" required maxLength={10000} rows={8} value={conteudo} onChange={e => setConteudo(e.target.value)} disabled={enviando} />
      <p className="mt-1 text-right text-xs text-muted-foreground">{conteudo.length}/10.000</p>
    </div>
    {erro && <p role="alert" className="text-sm text-destructive">{erro}</p>}
    <div className="flex flex-wrap justify-end gap-3">
      <button type="button" onClick={onCancelar} disabled={enviando} className="btn btn-outline">Cancelar</button>
      <button disabled={enviando || carregando || !!erroDisciplinas || !disciplinas.length} className="btn btn-primary">{enviando ? 'Salvando...' : inicial ? 'Salvar alterações' : 'Publicar discussão'}</button>
    </div>
  </form>
}
