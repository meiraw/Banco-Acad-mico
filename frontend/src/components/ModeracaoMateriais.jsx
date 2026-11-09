import { useEffect, useRef, useState } from 'react'
import { api, tamanho } from '../lib/api.js'
import { Modal } from './Modal.jsx'
import { VisualizadorMaterial } from './VisualizadorMaterial.jsx'
import { DadosPublicacao } from './DadosPublicacao.jsx'

export function ModeracaoMateriais() {
  const [pagina, setPagina] = useState(0)
  const [tentativa, setTentativa] = useState(0)
  const [resultado, setResultado] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [erroDecisao, setErroDecisao] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [visualizando, setVisualizando] = useState(null)
  const [decisao, setDecisao] = useState(null)
  const [motivo, setMotivo] = useState('')
  const [salvando, setSalvando] = useState(false)
  const envio = useRef(false)

  useEffect(() => {
    let ativo = true
    setCarregando(true); setErro('')
    api.materiaisPendentes(pagina).then(r => {
      if (!ativo) return
      if (!r.content.length && pagina > 0) setPagina(p => p - 1)
      else setResultado(r)
    }).catch(e => { if (ativo) setErro(e.message) })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [pagina, tentativa])

  function abrir(material, aprovar) {
    setDecisao({ material, aprovar }); setMotivo(''); setErroDecisao(''); setMensagem('')
  }
  async function decidir(event) {
    event.preventDefault()
    if (envio.current) return
    if (!decisao.aprovar && !motivo.trim()) { setErroDecisao('Informe o motivo da rejeição.'); return }
    envio.current = true; setSalvando(true); setErroDecisao('')
    try {
      if (decisao.aprovar) await api.aprovarMaterial(decisao.material.id)
      else await api.rejeitarMaterial(decisao.material.id, motivo.trim())
      setMensagem(decisao.aprovar ? 'Material aprovado.' : 'Material rejeitado.')
      setDecisao(null); setTentativa(t => t + 1)
    } catch (e) {
      if (e.status === 409) { setDecisao(null); setMensagem(e.message); setTentativa(t => t + 1) }
      else setErroDecisao(e.message)
    } finally { envio.current = false; setSalvando(false) }
  }

  return <section aria-labelledby="moderacao-titulo" className="space-y-4">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <h3 id="moderacao-titulo" className="text-xl font-semibold">Moderação de materiais</h3>
      <button type="button" disabled={carregando || salvando} onClick={() => setTentativa(t => t + 1)} className="btn btn-outline">Atualizar fila</button>
    </div>
    <p className="text-sm text-muted-foreground">Analise as publicações antes de disponibilizá-las. A aprovação administrativa não garante que o conteúdo esteja livre de restrições legais.</p>
    {mensagem && <p role="status" className="text-sm text-secondary">{mensagem}</p>}
    {erro && <p role="alert" className="text-sm text-destructive">{erro}</p>}
    {carregando ? <p role="status">Carregando materiais pendentes…</p> : !erro && <>
      {!resultado?.content.length && <p className="card-surface p-5 text-muted-foreground">Nenhum material aguardando aprovação.</p>}
      <div className="grid gap-4 lg:grid-cols-2">
        {resultado?.content.map(m => <article key={m.id} className="card-surface min-w-0 space-y-3 p-5">
          <h4 className="break-words text-lg font-semibold">{m.titulo}</h4>
          <p className="whitespace-pre-wrap break-words text-sm text-muted-foreground">{m.descricao}</p>
          <DadosPublicacao material={m} />
          <dl className="space-y-1 break-words text-sm">
            <div><dt className="inline font-semibold">Disciplina: </dt><dd className="inline">{m.disciplinaNome}</dd></div>
            <div><dt className="inline font-semibold">Autor: </dt><dd className="inline">{m.autorNome || 'Autor não informado'}</dd></div>
            <div><dt className="inline font-semibold">Enviado em: </dt><dd className="inline">{m.publicadoEm ? new Date(m.publicadoEm).toLocaleString('pt-BR') : 'Não informado'}</dd></div>
            <div><dt className="inline font-semibold">Arquivo: </dt><dd className="inline">{m.nomeArquivo} · {m.tipoArquivo} · {tamanho(m.tamanhoArquivo)}</dd></div>
          </dl>
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={() => setVisualizando(m)} className="btn btn-outline">Visualizar</button>
            <button type="button" disabled={salvando} onClick={() => abrir(m, true)} className="btn btn-primary">Aprovar</button>
            <button type="button" disabled={salvando} onClick={() => abrir(m, false)} className="btn btn-outline text-destructive">Rejeitar</button>
          </div>
        </article>)}
      </div>
      {resultado && (pagina > 0 || !resultado.last) && <nav aria-label="Páginas da fila de moderação" className="flex items-center justify-between gap-3">
        <button type="button" disabled={pagina === 0} onClick={() => setPagina(p => p - 1)} className="btn btn-outline">Anterior</button>
        <span className="text-sm">Página {pagina + 1}</span>
        <button type="button" disabled={resultado.last} onClick={() => setPagina(p => p + 1)} className="btn btn-outline">Próxima</button>
      </nav>}
    </>}
    {visualizando && <VisualizadorMaterial material={visualizando} disciplina={{ nome: visualizando.disciplinaNome }} onClose={() => setVisualizando(null)} />}
    {decisao && <Modal titulo={decisao.aprovar ? 'Aprovar publicação' : 'Rejeitar publicação'} onClose={() => !envio.current && setDecisao(null)}>
      <form onSubmit={decidir} className="space-y-4">
        <p className="break-words text-sm">{decisao.aprovar ? 'Confirma a aprovação de' : 'Informe o motivo da rejeição de'} “{decisao.material.titulo}”?</p>
        {!decisao.aprovar && <div>
          <label htmlFor="motivo-rejeicao" className="mb-2 block text-sm">Justificativa (visível ao autor)</label>
          <textarea id="motivo-rejeicao" required maxLength={2000} rows={4} disabled={salvando} value={motivo} onChange={e => setMotivo(e.target.value)} className="input-base h-auto py-2" />
        </div>}
        {erroDecisao && <p role="alert" className="text-sm text-destructive">{erroDecisao}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" disabled={salvando} onClick={() => setDecisao(null)} className="btn btn-ghost">Cancelar</button>
          <button disabled={salvando} className="btn btn-primary">{salvando ? 'Salvando…' : decisao.aprovar ? 'Confirmar aprovação' : 'Confirmar rejeição'}</button>
        </div>
      </form>
    </Modal>}
  </section>
}
