import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../lib/api.js'
import { MaterialCard } from './MaterialCard.jsx'
import { Modal } from './Modal.jsx'
import { useFavoritos } from '../lib/useFavoritos.js'

export function MinhasPublicacoes({ cursos, semestres }) {
  const [pagina, setPagina] = useState(0)
  const [resultado, setResultado] = useState(null)
  const [disciplinas, setDisciplinas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [tentativa, setTentativa] = useState(0)
  const [removendo, setRemovendo] = useState(null)
  const [excluindo, setExcluindo] = useState(false)
  const [erroExclusao, setErroExclusao] = useState('')
  const [mensagem, setMensagem] = useState('')
  const exclusao = useRef(false)
  const { isFavorito, alternar } = useFavoritos()

  async function excluir() {
    if (exclusao.current || !removendo) return
    exclusao.current = true; setExcluindo(true); setErroExclusao('')
    try {
      await api.excluirMaterial(removendo.id)
      // Uma falha no armazenamento local não desfaz a exclusão confirmada no servidor.
      try { if (isFavorito(removendo.id)) alternar(removendo.id) } catch { /* favoritos locais indisponíveis */ }
      setRemovendo(null); setMensagem('Publicação excluída. O arquivo associado também será removido.')
      setTentativa(t => t + 1)
    } catch (e) {
      if (e.status === 404) {
        setRemovendo(null); setMensagem('Esta publicação já foi removida.'); setTentativa(t => t + 1)
      } else setErroExclusao(e.message)
    } finally { exclusao.current = false; setExcluindo(false) }
  }

  useEffect(() => {
    let ativo = true
    setCarregando(true); setErro('')
    Promise.all([api.minhasPublicacoes(pagina), api.disciplinas()])
      .then(([publicacoes, d]) => { if (ativo) {
        if (!publicacoes.content.length && pagina > 0) setPagina(p => p - 1)
        else { setResultado(publicacoes); setDisciplinas(d) }
      } })
      .catch(e => { if (ativo) setErro(e.message) })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [pagina, tentativa])

  return <section aria-labelledby="minhas-publicacoes" className="mt-8 space-y-4">
    <div>
      <h2 id="minhas-publicacoes" className="text-2xl font-bold tracking-tight">Minhas publicações</h2>
      <p className="mt-1 text-sm text-muted-foreground">Os materiais que você compartilhou, com a disciplina e o semestre de cada publicação.</p>
    </div>
    {mensagem && <p role="status" className="text-sm text-secondary">{mensagem}</p>}
    {carregando ? <p role="status" className="text-sm text-muted-foreground">Carregando suas publicações…</p>
      : erro ? <div role="alert" className="card-surface p-5"><p className="text-sm text-destructive">{erro}</p><button type="button" onClick={() => setTentativa(t => t + 1)} className="btn btn-outline mt-3">Tentar novamente</button></div>
        : !resultado?.content.length ? <div className="card-surface p-6 text-center">
          <p className="font-medium">{pagina === 0 ? 'Você ainda não publicou materiais.' : 'Não há publicações nesta página.'}</p>
          <Link to="/compartilhar" className="btn btn-primary mt-4">Compartilhar material</Link>
        </div>
          : <>
            <p className="label-mono text-muted-foreground">{resultado.totalElements} {resultado.totalElements === 1 ? 'publicação' : 'publicações'}</p>
            <div className="grid gap-4 sm:grid-cols-2 2xl:grid-cols-3">
              {resultado.content.map(material => {
                const disciplina = disciplinas.find(d => d.id === material.disciplinaId)
                const indice = semestres.findIndex(s => s.id === disciplina?.semestreId)
                return <MaterialCard key={material.id} material={material} disciplina={disciplina} cursos={cursos}
                  onExcluir={m => { setRemovendo(m); setErroExclusao(''); setMensagem('') }}
                  semestre={semestres[indice]} semestreIndice={indice >= 0 ? indice : undefined} />
              })}
            </div>
          </>}
    {!carregando && !erro && resultado && (pagina > 0 || !resultado.last) && <nav aria-label="Páginas das minhas publicações" className="flex items-center justify-between gap-3">
      <button type="button" disabled={pagina === 0} onClick={() => setPagina(p => p - 1)} className="btn btn-outline">Anterior</button>
      <span className="text-sm text-muted-foreground">Página {pagina + 1}</span>
      <button type="button" disabled={resultado.last} onClick={() => setPagina(p => p + 1)} className="btn btn-outline">Próxima</button>
    </nav>}
    {removendo && <Modal titulo="Excluir publicação" onClose={() => !exclusao.current && setRemovendo(null)}>
      <p className="break-words text-sm">Excluir “{removendo.titulo}”? A publicação, o arquivo associado, os comentários e as curtidas serão removidos. Esta ação não pode ser desfeita.</p>
      {erroExclusao && <p role="alert" className="mt-3 text-sm text-destructive">{erroExclusao}</p>}
      <div className="mt-5 flex justify-end gap-2">
        <button type="button" disabled={excluindo} onClick={() => setRemovendo(null)} className="btn btn-ghost">Cancelar</button>
        <button type="button" disabled={excluindo} onClick={excluir} className="btn btn-primary">{excluindo ? 'Excluindo…' : 'Excluir publicação'}</button>
      </div>
    </Modal>}
  </section>
}
