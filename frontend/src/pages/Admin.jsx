import { useEffect, useState } from 'react'
import { ShieldCheck, Plus, Pencil, Trash2, GraduationCap, CalendarDays, BookOpen, Check } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { Modal } from '../components/Modal.jsx'
import { api, rotuloSemestre } from '../lib/api.js'

const abas = [
  { id: 'curso', titulo: 'Cursos', singular: 'curso', Icone: GraduationCap },
  { id: 'semestre', titulo: 'Semestres', singular: 'semestre', Icone: CalendarDays },
  { id: 'disciplina', titulo: 'Disciplinas', singular: 'disciplina', Icone: BookOpen },
]
const operacoes = {
  curso: [api.criarCurso, api.atualizarCurso, api.excluirCurso],
  semestre: [api.criarSemestre, api.atualizarSemestre, api.excluirSemestre],
  disciplina: [api.criarDisciplina, api.atualizarDisciplina, api.excluirDisciplina],
}

export function Admin() {
  const [dados, setDados] = useState({ curso: [], semestre: [], disciplina: [] })
  const [aba, setAba] = useState('curso')
  const [cursoFiltro, setCursoFiltro] = useState('')
  const [busca, setBusca] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [editor, setEditor] = useState(null)
  const [removendo, setRemovendo] = useState(null)
  const [ocupado, setOcupado] = useState(false)

  async function carregar() {
    const [curso, semestre, disciplina] = await Promise.all([api.cursos(), api.semestres(), api.disciplinas()])
    setDados({ curso, semestre, disciplina })
  }
  useEffect(() => {
    let ativo = true
    Promise.all([api.cursos(), api.semestres(), api.disciplinas()])
      .then(([curso, semestre, disciplina]) => { if (ativo) setDados({ curso, semestre, disciplina }) })
      .catch(e => { if (ativo) setErro(e.message) })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [])
  useEffect(() => {
    const anterior = document.title
    document.title = 'Painel administrativo | Banco Acadêmico'
    return () => { document.title = anterior }
  }, [])

  const atual = abas.find(a => a.id === aba)
  const cursoDe = item => aba === 'curso' ? item.id : aba === 'semestre' ? item.cursoId : dados.semestre.find(s => s.id === item.semestreId)?.cursoId
  const registros = dados[aba].filter(item => (!cursoFiltro || cursoDe(item) === cursoFiltro) && item.nome.toLocaleLowerCase('pt-BR').includes(busca.toLocaleLowerCase('pt-BR')))
  const podeCriar = aba === 'curso' || (aba === 'semestre' ? dados.curso.length > 0 : dados.semestre.length > 0)

  function abrir(item = null) {
    setErro(''); setMensagem('')
    setEditor({ tipo: aba, item })
  }
  async function excluir() {
    setOcupado(true); setErro('')
    try {
      await operacoes[removendo.tipo][2](removendo.item.id)
      setRemovendo(null)
      setMensagem('Registro excluído.')
      await carregar()
    } catch (e) { setErro(e.message) }
    finally { setOcupado(false) }
  }

  return <AppShell cursos={dados.curso} semestres={dados.semestre} rotuloSemestre={rotuloSemestre}>
    <div className="space-y-6">
      <header>
        <h1 className="flex items-center gap-2 text-3xl font-bold tracking-tight"><ShieldCheck className="size-7 text-primary" /> Painel administrativo</h1>
        <p className="mt-2 text-muted-foreground">Organize os cursos, semestres e disciplinas da grade curricular.</p>
      </header>
      <div className="grid grid-cols-3 gap-3">
        {abas.map(({ id, titulo, Icone }) => <div key={id} className="card-surface p-4"><Icone className="mb-2 size-5 text-primary" /><p className="text-2xl font-bold">{dados[id].length}</p><p className="label-mono text-muted-foreground">{titulo}</p></div>)}
      </div>
      <div role="tablist" aria-label="Grade curricular" className="flex flex-wrap gap-1 rounded-lg bg-surface-container p-1">
        {abas.map(a => <button key={a.id} id={`tab-${a.id}`} role="tab" aria-selected={aba === a.id} aria-controls="grade-conteudo" onClick={() => { setAba(a.id); setBusca(''); setErro(''); setMensagem('') }} className={`btn flex-1 ${aba === a.id ? 'bg-card text-primary shadow-sm' : 'btn-ghost text-muted-foreground'}`}><a.Icone className="size-4" />{a.titulo}</button>)}
      </div>
      {mensagem && <p role="status" className="flex items-center gap-2 text-sm text-secondary"><Check className="size-4" />{mensagem}</p>}
      {erro && !removendo && <p role="alert" className="text-sm text-destructive">{erro} <button className="underline" onClick={() => { setErro(''); carregar().catch(e => setErro(e.message)) }}>Tentar novamente</button></p>}
      <section id="grade-conteudo" role="tabpanel" aria-labelledby={`tab-${aba}`} className="space-y-4">
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-40 flex-1"><label htmlFor="admin-busca" className="label-mono mb-1 block">Buscar {atual.singular}</label><input id="admin-busca" value={busca} onChange={e => setBusca(e.target.value)} placeholder="Buscar pelo nome…" className="input-base" /></div>
          <div><label htmlFor="admin-filtro" className="label-mono mb-1 block">Curso</label><select id="admin-filtro" value={cursoFiltro} onChange={e => setCursoFiltro(e.target.value)} className="input-base"><option value="">Todos os cursos</option>{dados.curso.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}</select></div>
          <button disabled={carregando || !podeCriar} onClick={() => abrir()} className="btn btn-primary"><Plus className="size-4" /> Cadastrar {atual.singular}</button>
        </div>
        {!podeCriar && <p className="text-sm text-muted-foreground">{aba === 'semestre' ? 'Cadastre um curso primeiro.' : 'Cadastre um semestre primeiro.'}</p>}
        <div className="card-surface overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-border bg-surface-container"><tr className="label-mono text-left text-muted-foreground"><th className="px-4 py-3">Nome</th>{aba !== 'curso' && <th className="px-4 py-3">Curso</th>}{aba === 'semestre' && <th className="px-4 py-3">Número</th>}{aba === 'disciplina' && <><th className="px-4 py-3">Semestre</th><th className="px-4 py-3">Descrição</th></>}<th className="px-4 py-3 text-right">Ações</th></tr></thead>
            <tbody>{registros.map(item => <tr key={item.id} className="border-b border-border last:border-0"><td className="px-4 py-3 font-medium">{item.nome}</td>{aba !== 'curso' && <td className="px-4 py-3">{dados.curso.find(c => c.id === cursoDe(item))?.nome || '—'}</td>}{aba === 'semestre' && <td className="px-4 py-3">{item.numero}º</td>}{aba === 'disciplina' && <><td className="px-4 py-3">{dados.semestre.find(s => s.id === item.semestreId)?.numero}º semestre</td><td className="max-w-xs px-4 py-3 text-muted-foreground">{item.descricao}</td></>}<td className="px-4 py-3"><div className="flex justify-end gap-1"><button className="btn btn-ghost btn-icon" aria-label={`Editar ${item.nome}`} onClick={() => abrir(item)}><Pencil className="size-4" /></button><button className="btn btn-ghost btn-icon text-destructive" aria-label={`Excluir ${item.nome}`} onClick={() => { setErro(''); setRemovendo({ tipo: aba, item }) }}><Trash2 className="size-4" /></button></div></td></tr>)}</tbody>
          </table>
          {(carregando || !registros.length) && <div className="p-8 text-center text-muted-foreground">{carregando ? 'Carregando grade curricular…' : busca || cursoFiltro ? 'Nenhum registro encontrado para estes filtros.' : `Nenhum registro cadastrado. Comece cadastrando um ${atual.singular}.`}</div>}
        </div>
      </section>
    </div>
    {editor && <EditorGrade tipo={editor.tipo} item={editor.item} dados={dados} cursoInicial={cursoFiltro} onClose={() => setEditor(null)} onSalvo={async () => { setEditor(null); setMensagem('Registro salvo.'); try { await carregar() } catch (e) { setErro(`Registro salvo, mas não foi possível atualizar a lista: ${e.message}`) } }} />}
    {removendo && <Modal titulo="Excluir registro" onClose={() => !ocupado && setRemovendo(null)}><p className="text-sm text-muted-foreground">Excluir “{removendo.item.nome}”? Esta ação não pode ser desfeita. Registros vinculados podem impedir a exclusão.</p>{erro && <p role="alert" className="mt-3 text-sm text-destructive">{erro}</p>}<div className="mt-6 flex justify-end gap-2"><button disabled={ocupado} className="btn btn-ghost" onClick={() => setRemovendo(null)}>Cancelar</button><button disabled={ocupado} className="btn bg-destructive text-white" onClick={excluir}>{ocupado ? 'Excluindo…' : 'Excluir'}</button></div></Modal>}
  </AppShell>
}

function EditorGrade({ tipo, item, dados, cursoInicial, onClose, onSalvo }) {
  const [form, setForm] = useState({ nome: item?.nome || '', descricao: item?.descricao || '', numero: item?.numero || 1, cursoId: item?.cursoId || cursoInicial || '', semestreId: item?.semestreId || '' })
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)
  const mudar = campo => e => setForm(f => ({ ...f, [campo]: e.target.value }))
  const cursoDisciplina = dados.semestre.find(s => s.id === form.semestreId)?.cursoId || form.cursoId
  const semestres = dados.semestre.filter(s => !cursoDisciplina || s.cursoId === cursoDisciplina)
  async function salvar(e) {
    e.preventDefault()
    if (!form.nome.trim() || (tipo === 'disciplina' && !form.descricao.trim())) { setErro('Preencha os campos sem usar apenas espaços.'); return }
    const payload = tipo === 'curso' ? { nome: form.nome.trim() } : tipo === 'semestre' ? { nome: form.nome.trim(), numero: Number(form.numero), cursoId: form.cursoId } : { nome: form.nome.trim(), descricao: form.descricao.trim(), semestreId: form.semestreId }
    setSalvando(true); setErro('')
    try { if (item) await operacoes[tipo][1](item.id, payload); else await operacoes[tipo][0](payload); await onSalvo() }
    catch (e) { setErro(e.message) }
    finally { setSalvando(false) }
  }
  return <Modal titulo={`${item ? 'Editar' : 'Cadastrar'} ${tipo}`} onClose={() => !salvando && onClose()}>
    <form onSubmit={salvar} className="space-y-4">
      {tipo !== 'curso' && <div><label htmlFor="grade-curso" className="label-mono mb-1 block">Curso</label><select id="grade-curso" required value={tipo === 'disciplina' ? cursoDisciplina : form.cursoId} onChange={e => setForm(f => ({ ...f, cursoId: e.target.value, semestreId: '' }))} className="input-base"><option value="">Selecione um curso</option>{dados.curso.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}</select></div>}
      {tipo === 'disciplina' && <div><label htmlFor="grade-semestre" className="label-mono mb-1 block">Semestre</label><select id="grade-semestre" required value={form.semestreId} onChange={mudar('semestreId')} className="input-base"><option value="">Selecione um semestre</option>{semestres.map(s => <option key={s.id} value={s.id}>{rotuloSemestre(s)}</option>)}</select></div>}
      {tipo === 'semestre' && <div><label htmlFor="grade-numero" className="label-mono mb-1 block">Número do semestre</label><input id="grade-numero" required type="number" min="1" max="8" step="1" value={form.numero} onChange={mudar('numero')} className="input-base" /></div>}
      <div><label htmlFor="grade-nome" className="label-mono mb-1 block">Nome</label><input autoFocus id="grade-nome" required maxLength={tipo === 'semestre' ? 100 : 200} value={form.nome} onChange={mudar('nome')} className="input-base" /></div>
      {tipo === 'disciplina' && <div><label htmlFor="grade-descricao" className="label-mono mb-1 block">Descrição</label><textarea id="grade-descricao" required maxLength={255} rows="3" value={form.descricao} onChange={mudar('descricao')} className="input-base" /></div>}
      {erro && <p role="alert" className="text-sm text-destructive">{erro}</p>}
      <div className="flex justify-end gap-2"><button type="button" disabled={salvando} onClick={onClose} className="btn btn-ghost">Cancelar</button><button disabled={salvando} className="btn btn-primary">{salvando ? 'Salvando…' : 'Salvar'}</button></div>
    </form>
  </Modal>
}
