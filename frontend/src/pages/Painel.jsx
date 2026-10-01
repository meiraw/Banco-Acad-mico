import { useEffect, useMemo, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { Search, Sparkles, Upload, ChevronRight, Star, PlusCircle } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { MaterialCard } from '../components/MaterialCard.jsx'
import { VisualizadorMaterial } from '../components/VisualizadorMaterial.jsx'
import { NovoCurso, NovoSemestre, NovaDisciplina } from '../components/Gerenciar.jsx'
import { api, categoria, rotuloSemestre } from '../lib/api.js'
import { useSessao } from '../lib/sessao.jsx'

const TIPOS = ['Todos', 'PDF', 'Slides', 'Imagem', 'Outro']

export function Painel() {
  const { perfil } = useSessao()
  const [params, setParams] = useSearchParams()
  const semestreId = params.get('semestre')
  const cursoId = params.get('curso')
  const disciplinaId = params.get('disciplina')
  const [cursos, setCursos] = useState([])

  const [semestres, setSemestres] = useState([])
  const [disciplinas, setDisciplinas] = useState([])
  const [materiais, setMateriais] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [termo, setTermo] = useState('')
  const [tipo, setTipo] = useState('Todos')
  const [previa, setPrevia] = useState(null)
  const [modal, setModal] = useState(null)

  async function carregar() {
    try {
      const [c, s, d, m] = await Promise.all([api.cursos(), api.semestres(), api.disciplinas(), api.materiais()])
      setCursos(c)
      setSemestres(s)
      setDisciplinas(d); setMateriais(m); setErro('')
    } catch (e) { setErro(e.message) } finally { setCarregando(false) }
  }
  useEffect(() => { carregar() }, [])

  const discPorId = useMemo(() => Object.fromEntries(disciplinas.map(d => [d.id, d])), [disciplinas])
  const semestreIndicePorDisc = useMemo(() => Object.fromEntries(disciplinas.map(d => [d.id, semestres.findIndex(s => s.id === d.semestreId)])), [disciplinas, semestres])
  const semestresVisiveis = semestres.filter(s => !cursoId || s.cursoId === cursoId)
  const semestresIds = new Set(semestresVisiveis.map(s => s.id))
  const discVisiveis = disciplinas.filter(d => semestresIds.has(d.semestreId) && (!semestreId || d.semestreId === semestreId))
  const disciplinasIds = new Set(discVisiveis.map(d => d.id))
  function selecionarSemestre(id) {
    setParams({ ...(cursoId ? { curso: cursoId } : {}), ...(id ? { semestre: id } : {}) })
  }

  const q = termo.trim().toLowerCase()
  const filtrados = materiais.filter(m => {
    const d = discPorId[m.disciplinaId]
    if (!disciplinasIds.has(m.disciplinaId)) return false
    if (disciplinaId && m.disciplinaId !== disciplinaId) return false
    if (tipo !== 'Todos' && categoria(m.tipoArquivo) !== tipo) return false
    return !q || `${m.titulo} ${d?.nome ?? ''}`.toLowerCase().includes(q)
  })

  const urlCompartilhar = `/compartilhar?${new URLSearchParams({ ...(cursoId ? { curso: cursoId } : {}), ...(semestreId ? { semestre: semestreId } : {}) })}`
  const primeiroNome = perfil?.nome?.split(' ')[0] ?? 'Acadêmico'
  const fechar = () => setModal(null)
  const salvo = () => { fechar(); carregar() }

  async function excluir(m) {
    try { await api.excluirMaterial(m.id); setPrevia(null); carregar() } catch (e) { setErro(e.message) }
  }

  return (
    <AppShell cursos={cursos} semestres={semestresVisiveis} rotuloSemestre={rotuloSemestre}>
      <div className="grid gap-6 xl:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <div>
            <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Olá, {primeiroNome}! Seja bem-vindo</h1>
            <p className="mt-1 text-muted-foreground">Encontre e compartilhe materiais das disciplinas do seu curso.</p>
          </div>

          <div className="relative">
            <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-muted-foreground" />
            <input
              value={termo}
              onChange={e => setTermo(e.target.value)}
              placeholder="Buscar por título ou disciplina"
              aria-label="Pesquisar materiais"
              className="input-base h-12 rounded-full pl-12"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <label htmlFor="filtro-curso" className="label-mono mr-1 text-muted-foreground">Curso</label>
            <select id="filtro-curso" value={cursoId || ''} onChange={e => setParams(e.target.value ? { curso: e.target.value } : {})} className="input-base w-auto max-w-full">
              <option value="">Todos os cursos</option>
              {cursos.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
            <button onClick={() => setModal('curso')} className="chip flex items-center gap-1 border-dashed text-primary"><PlusCircle className="size-3.5" /> Curso</button>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <span className="label-mono mr-1 text-muted-foreground">Semestre</span>
            <button onClick={() => selecionarSemestre(null)} className={`chip ${!semestreId ? 'on' : ''}`}>Todos</button>
            {semestresVisiveis.map(s => (
              <button key={s.id} onClick={() => selecionarSemestre(s.id)} className={`chip ${semestreId === s.id ? 'on' : ''}`}>{rotuloSemestre(s)}{!cursoId && ` · ${cursos.find(c => c.id === s.cursoId)?.nome || ''}`}</button>
            ))}
            <button onClick={() => setModal('semestre')} className="chip flex items-center gap-1 border-dashed text-primary"><PlusCircle className="size-3.5" /> Semestre</button>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <label htmlFor="filtro-disciplina" className="label-mono mr-1 text-muted-foreground">Disciplina</label>
            <select id="filtro-disciplina" value={disciplinaId || ''} onChange={e => {
              const next = new URLSearchParams(params)
              if (e.target.value) next.set('disciplina', e.target.value)
              else next.delete('disciplina')
              setParams(next)
            }} className="input-base w-auto max-w-full">
              <option value="">Todas as disciplinas</option>
              {discVisiveis.map(d => <option key={d.id} value={d.id}>{d.nome}</option>)}
            </select>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <span className="label-mono mr-1 text-muted-foreground">Tipo</span>
            {TIPOS.map(t => <button key={t} onClick={() => setTipo(t)} className={`chip ${tipo === t ? 'on' : ''}`}>{t}</button>)}
            <div className="ml-auto">
              <Link to={urlCompartilhar} className="btn btn-secondary"><Upload className="size-4" /> Compartilhar material</Link>
            </div>
          </div>

          {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}

          {carregando ? (
            <p className="label-mono text-muted-foreground">Carregando materiais…</p>
          ) : filtrados.length === 0 ? (
            <div className="card-surface p-8 text-center">
              <p className="font-medium">{materiais.length ? 'Nenhum material encontrado' : 'Ainda não há materiais'}</p>
              <p className="mt-1 text-sm text-muted-foreground">{materiais.length ? 'Tente outro termo ou limpe os filtros.' : 'Seja o primeiro a contribuir com a turma.'}</p>
              <Link to={urlCompartilhar} className="btn btn-primary mt-4 inline-flex">Compartilhar material</Link>
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 2xl:grid-cols-3">
              {filtrados.map(m => (
                <MaterialCard
                  key={m.id}
                  material={m}
                  disciplina={discPorId[m.disciplinaId]}
                  semestreIndice={semestreIndicePorDisc[m.disciplinaId]}
                  semestre={semestres.find(s => s.id === discPorId[m.disciplinaId]?.semestreId)}
                  cursos={cursos}
                  onVisualizar={setPrevia}
                  onExcluir={excluir}
                />
              ))}
            </div>
          )}
        </div>

        <aside className="space-y-4">
          <section className="card-surface p-5">
            <h2 className="flex items-center gap-2 font-semibold text-primary"><Star className="size-5" /> Seus favoritos</h2>
            <p className="mt-1 text-sm text-muted-foreground">Materiais salvos com o coração ficam reunidos aqui.</p>
            <Link to="/favoritos" className="btn btn-outline mt-3 w-full">Abrir favoritos</Link>
          </section>

          <section className="card-surface bg-muted p-5">
            <h2 className="flex items-center gap-2 font-semibold text-primary"><Sparkles className="size-5" /> Disciplinas em destaque</h2>
            <div className="mt-3 space-y-2">
              {discVisiveis.slice(0, 4).map(d => (
                <button key={d.id} onClick={() => setTermo(d.nome)} className="flex w-full items-center justify-between rounded-lg bg-card px-3 py-2.5 text-left transition-colors hover:bg-surface-container">
                  <span className="text-sm font-medium">{d.nome}</span>
                  <ChevronRight className="size-4 text-muted-foreground" />
                </button>
              ))}
              {!discVisiveis.length && <p className="text-sm text-muted-foreground">Nenhuma disciplina cadastrada ainda.</p>}
            </div>
            <button onClick={() => setModal('disciplina')} className="btn btn-outline mt-3 w-full"><PlusCircle className="size-4" /> Nova disciplina</button>
          </section>
        </aside>
      </div>

      <VisualizadorMaterial material={previa} disciplina={previa && discPorId[previa.disciplinaId]} onClose={() => setPrevia(null)} onExcluir={excluir} />
      {modal === 'curso' && <NovoCurso onClose={fechar} onSalvo={salvo} />}
      {modal === 'semestre' && <NovoSemestre cursos={cursos} cursoInicial={cursoId} onClose={fechar} onSalvo={salvo} />}
      {modal === 'disciplina' && <NovaDisciplina cursos={cursos} semestres={semestresVisiveis} semestreInicial={semestreId} onClose={fechar} onSalvo={salvo} />}
    </AppShell>
  )
}
