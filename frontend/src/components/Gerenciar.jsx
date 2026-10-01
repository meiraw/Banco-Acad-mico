import { useState } from 'react'
import { Modal } from './Modal.jsx'
import { api, rotuloSemestre } from '../lib/api.js'

export function NovoCurso({ onClose, onSalvo }) {
  const [nome, setNome] = useState('')
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)
  async function salvar(e) {
    e.preventDefault()
    if (!nome.trim()) return
    setSalvando(true)
    try { await api.criarCurso({ nome: nome.trim() }); onSalvo() }
    catch (err) { setErro(err.message); setSalvando(false) }
  }
  return <Modal titulo="Novo curso" onClose={onClose}>
    <form onSubmit={salvar} className="space-y-4">
      <label className="label-mono block" htmlFor="curso-nome">Nome do curso</label>
      <input id="curso-nome" required maxLength={200} value={nome} onChange={e => setNome(e.target.value)} className="input-base" />
      {erro && <p role="alert" className="text-sm text-destructive">{erro}</p>}
      <button disabled={salvando} className="btn btn-primary w-full">{salvando ? 'Salvando…' : 'Criar curso'}</button>
    </form>
  </Modal>
}

export function NovoSemestre({ cursos, cursoInicial, onClose, onSalvo }) {
  const [d, setD] = useState({ numero: 1, nome: '', cursoId: cursoInicial || '' })
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)
  const set = k => e => setD({ ...d, [k]: e.target.value })
  async function salvar(e) {
    e.preventDefault()
    if (!d.nome.trim()) { setErro('Informe o nome do semestre.'); return }
    setSalvando(true)
    try { await api.criarSemestre({ numero: +d.numero, nome: d.nome.trim(), cursoId: d.cursoId }); onSalvo() }
    catch (err) { setErro(err.message); setSalvando(false) }
  }
  return (
    <Modal titulo="Novo semestre" onClose={onClose}>
      <form onSubmit={salvar} className="space-y-4">
        <div className="space-y-1.5">
          <label htmlFor="semestre-curso" className="label-mono block">Curso</label>
          <select id="semestre-curso" required value={d.cursoId} onChange={set('cursoId')} className="input-base">
            <option value="">Selecione…</option>
            {cursos.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select>
        </div>
        <div className="space-y-1.5">
          <label htmlFor="semestre-numero" className="label-mono block">Número do semestre</label>
          <input id="semestre-numero" required type="number" min="1" max="8" value={d.numero} onChange={set('numero')} className="input-base" />
        </div>
        <div className="space-y-1.5">
          <label htmlFor="semestre-nome" className="label-mono block">Nome</label>
          <input id="semestre-nome" required maxLength={100} value={d.nome} onChange={set('nome')} className="input-base" />
        </div>
        {!cursos.length && <p className="text-sm text-muted-foreground">Cadastre um curso no painel primeiro.</p>}
        {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}
        <button disabled={salvando || !cursos.length} className="btn btn-primary w-full">{salvando ? 'Salvando…' : 'Criar semestre'}</button>
      </form>
    </Modal>
  )
}

export function NovaDisciplina({ cursos, semestres, semestreInicial, onClose, onSalvo }) {
  const [d, setD] = useState({ nome: '', descricao: '', semestreId: semestreInicial || '' })
  const [erro, setErro] = useState('')
  const set = k => e => setD({ ...d, [k]: e.target.value })
  async function salvar(e) {
    e.preventDefault()
    if (!d.nome.trim() || !d.descricao.trim()) { setErro('Informe nome e descrição.'); return }
    try { await api.criarDisciplina({ ...d, nome: d.nome.trim(), descricao: d.descricao.trim() }); onSalvo() } catch (err) { setErro(err.message) }
  }
  return (
    <Modal titulo="Nova disciplina" onClose={onClose}>
      <form onSubmit={salvar} className="space-y-4">
        <div className="space-y-1.5">
          <label className="label-mono block">Semestre</label>
          <select required value={d.semestreId} onChange={set('semestreId')} className="input-base">
            <option value="">Selecione…</option>
            {semestres.map(s => <option key={s.id} value={s.id}>{rotuloSemestre(s)} · {cursos.find(c => c.id === s.cursoId)?.nome}</option>)}
          </select>
        </div>
        <div className="space-y-1.5">
          <label className="label-mono block">Nome</label>
          <input required value={d.nome} onChange={set('nome')} className="input-base" />
        </div>
        <div className="space-y-1.5">
          <label className="label-mono block">Descrição</label>
          <input required value={d.descricao} onChange={set('descricao')} className="input-base" />
        </div>
        {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}
        <button disabled={!semestres.length} className="btn btn-primary w-full">Criar disciplina</button>
        {!semestres.length && <p className="text-sm text-muted-foreground">Cadastre um semestre para este curso primeiro.</p>}
      </form>
    </Modal>
  )
}
