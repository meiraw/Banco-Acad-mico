import { useState } from 'react'
import { Modal } from './Modal.jsx'
import { api, rotuloSemestre } from '../lib/api.js'

export function NovoSemestre({ onClose, onSalvo }) {
  const [d, setD] = useState({ ano: new Date().getFullYear(), periodo: 1 })
  const [erro, setErro] = useState('')
  async function salvar(e) {
    e.preventDefault()
    try { await api.criarSemestre({ ano: +d.ano, periodo: +d.periodo }); onSalvo() } catch (err) { setErro(err.message) }
  }
  return (
    <Modal titulo="Novo semestre" onClose={onClose}>
      <form onSubmit={salvar} className="space-y-4">
        <div className="space-y-1.5">
          <label className="label-mono block">Ano</label>
          <input required type="number" min="2000" value={d.ano} onChange={e => setD({ ...d, ano: e.target.value })} className="input-base" />
        </div>
        <div className="space-y-1.5">
          <label className="label-mono block">Período</label>
          <select value={d.periodo} onChange={e => setD({ ...d, periodo: e.target.value })} className="input-base">
            <option value="1">1º</option><option value="2">2º</option>
          </select>
        </div>
        {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}
        <button className="btn btn-primary w-full">Criar semestre</button>
      </form>
    </Modal>
  )
}

export function NovaDisciplina({ semestres, semestreInicial, onClose, onSalvo }) {
  const [d, setD] = useState({ nome: '', descricao: '', semestreId: semestreInicial || '' })
  const [erro, setErro] = useState('')
  const set = k => e => setD({ ...d, [k]: e.target.value })
  async function salvar(e) {
    e.preventDefault()
    try { await api.criarDisciplina(d); onSalvo() } catch (err) { setErro(err.message) }
  }
  return (
    <Modal titulo="Nova disciplina" onClose={onClose}>
      <form onSubmit={salvar} className="space-y-4">
        <div className="space-y-1.5">
          <label className="label-mono block">Semestre</label>
          <select required value={d.semestreId} onChange={set('semestreId')} className="input-base">
            <option value="">Selecione…</option>
            {semestres.map(s => <option key={s.id} value={s.id}>{rotuloSemestre(s)}</option>)}
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
        <button className="btn btn-primary w-full">Criar disciplina</button>
      </form>
    </Modal>
  )
}
