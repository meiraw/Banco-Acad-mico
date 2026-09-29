import { useEffect, useMemo, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { Search, Sparkles, Upload, ChevronRight, Star, PlusCircle } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { MaterialCard } from '../components/MaterialCard.jsx'
import { VisualizadorMaterial } from '../components/VisualizadorMaterial.jsx'
import { NovoSemestre, NovaDisciplina } from '../components/Gerenciar.jsx'
import { api, categoria, rotuloSemestre } from '../lib/api.js'
import { useSessao } from '../lib/sessao.jsx'

const TIPOS = ['Todos', 'PDF', 'Slides', 'Imagem', 'Outro']

export function Painel() {
  const { perfil } = useSessao()
  const [params, setParams] = useSearchParams()
  const semestreId = params.get('semestre')

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
      const [s, d, m] = await Promise.all([api.semestres(), api.disciplinas(), api.materiais()])
      setSemestres(s.sort((a, b) => a.ano - b.ano || a.periodo - b.periodo))
      setDisciplinas(d); setMateriais(m); setErro('')
    } catch (e) { setErro(e.message) } finally { setCarregando(false) }
  }
  useEffect(() => { carregar() }, [])

  const discPorId = useMemo(() => Object.fromEntries(disciplinas.map(d => [d.id, d])), [disciplinas])
  const semestreIndicePorDisc = useMemo(() => Object.fromEntries(disciplinas.map(d => [d.id, semestres.findIndex(s => s.id === d.semestreId)])), [disciplinas, semestres])
  const discVisiveis = disciplinas.filter(d => !semestreId || d.semestreId === semestreId)

  const q = termo.trim().toLowerCase()
  const filtrados = materiais.filter(m => {
    const d = discPorId[m.disciplinaId]
    if (semestreId && d?.semestreId !== semestreId) return false
    if (tipo !== 'Todos' && categoria(m.tipoArquivo) !== tipo) return false
    return !q || `${m.titulo} ${d?.nome ?? ''}`.toLowerCase().includes(q)
  })

  const primeiroNome = perfil?.nome?.split(' ')[0] ?? 'Acadêmico'
  const fechar = () => setModal(null)
  const salvo = () => { fechar(); carregar() }

  async function excluir(m) {
    try { await api.excluirMaterial(m.id); setPrevia(null); carregar() } catch (e) { setErro(e.message) }
  }

  return (
    <AppShell semestres={semestres} rotuloSemestre={rotuloSemestre}>
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
            <span className="label-mono mr-1 text-muted-foreground">Semestre</span>
            <button onClick={() => setParams({})} className={`chip ${!semestreId ? 'on' : ''}`}>Todos</button>
            {semestres.map(s => (
              <button key={s.id} onClick={() => setParams({ semestre: s.id })} className={`chip ${semestreId === s.id ? 'on' : ''}`}>{rotuloSemestre(s)}</button>
            ))}
            <button onClick={() => setModal('semestre')} className="chip flex items-center gap-1 border-dashed text-primary"><PlusCircle className="size-3.5" /> Semestre</button>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <span className="label-mono mr-1 text-muted-foreground">Tipo</span>
            {TIPOS.map(t => <button key={t} onClick={() => setTipo(t)} className={`chip ${tipo === t ? 'on' : ''}`}>{t}</button>)}
            <div className="ml-auto">
              <Link to="/compartilhar" className="btn btn-secondary"><Upload className="size-4" /> Compartilhar material</Link>
            </div>
          </div>

          {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}

          {carregando ? (
            <p className="label-mono text-muted-foreground">Carregando materiais…</p>
          ) : filtrados.length === 0 ? (
            <div className="card-surface p-8 text-center">
              <p className="font-medium">{materiais.length ? 'Nenhum material encontrado' : 'Ainda não há materiais'}</p>
              <p className="mt-1 text-sm text-muted-foreground">{materiais.length ? 'Tente outro termo ou limpe os filtros.' : 'Seja o primeiro a contribuir com a turma.'}</p>
              <Link to="/compartilhar" className="btn btn-primary mt-4 inline-flex">Compartilhar material</Link>
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 2xl:grid-cols-3">
              {filtrados.map(m => (
                <MaterialCard
                  key={m.id}
                  material={m}
                  disciplina={discPorId[m.disciplinaId]}
                  semestreIndice={semestreIndicePorDisc[m.disciplinaId]}
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
      {modal === 'semestre' && <NovoSemestre onClose={fechar} onSalvo={salvo} />}
      {modal === 'disciplina' && <NovaDisciplina semestres={semestres} semestreInicial={semestreId} onClose={fechar} onSalvo={salvo} />}
    </AppShell>
  )
}
