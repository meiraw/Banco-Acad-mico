import { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { Search, SlidersHorizontal, Upload } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { MaterialCard } from '../components/MaterialCard.jsx'
import { api, categoria, rotuloSemestre } from '../lib/api.js'
import { useSessao } from '../lib/sessao.jsx'
import emptyMaterials from '../assets/illustrations/empty-materials.png'

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
  const [filtrosAbertos, setFiltrosAbertos] = useState(false)
  const buscaRef = useRef(null)

  useEffect(() => {
    if (!filtrosAbertos) return
    document.documentElement.classList.add('busca-filtros-abertos')
    return () => document.documentElement.classList.remove('busca-filtros-abertos')
  }, [filtrosAbertos])

  useEffect(() => {
    function fecharAoClicarFora(event) {
      if (!buscaRef.current?.contains(event.target)) setFiltrosAbertos(false)
    }
    document.addEventListener('pointerdown', fecharAoClicarFora)
    return () => document.removeEventListener('pointerdown', fecharAoClicarFora)
  }, [])

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
  // Curso, semestre e disciplina só são criados, e materiais só são excluídos, por administradores.
  const ehAdmin = perfil?.perfil === 'ADMIN'

  async function excluir(m) {
    if (!ehAdmin) return
    try { await api.excluirMaterial(m.id); carregar() } catch (e) { setErro(e.message) }
  }

  return (
    <AppShell cursos={cursos} semestres={semestresVisiveis} rotuloSemestre={rotuloSemestre}>
      <div>
        <div className="space-y-6">
          <div>
            <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Olá, {primeiroNome}! Seja bem-vindo</h1>
            <p className="mt-1 text-muted-foreground">Encontre e compartilhe materiais das disciplinas do seu curso.</p>
          </div>

          <div ref={buscaRef} onKeyDown={e => { if (e.key === 'Escape') { setFiltrosAbertos(false); e.stopPropagation() } }}>
          <div className="relative">
            <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-muted-foreground" />
            <input
              value={termo}
              onChange={e => setTermo(e.target.value)}
              onFocus={() => setFiltrosAbertos(true)}
              onClick={() => setFiltrosAbertos(true)}
              placeholder="Buscar por título ou disciplina"
              aria-label="Pesquisar materiais"
              aria-expanded={filtrosAbertos}
              aria-controls="filtros-busca"
              className="input-base h-12 rounded-full pl-12 pr-28"
            />
            <button type="button" onClick={() => setFiltrosAbertos(abertos => !abertos)} aria-expanded={filtrosAbertos} aria-controls="filtros-busca" className="btn btn-ghost absolute right-2 top-1/2 -translate-y-1/2 rounded-full text-primary">
              <SlidersHorizontal className="size-4" /> Filtros
              {(cursoId || semestreId || disciplinaId || tipo !== 'Todos') && <span className="size-2 rounded-full bg-primary" aria-label="Filtros ativos" />}
            </button>
          </div>

          {filtrosAbertos && <section id="filtros-busca" aria-label="Filtros de busca" className="card-surface mt-3 space-y-4 p-4 sm:p-5">
          <div className="flex items-center justify-between gap-3">
            <h2 className="font-semibold text-primary">Filtrar materiais</h2>
            <button type="button" className="btn btn-ghost text-sm" onClick={() => { setParams({}); setTipo('Todos') }}>Limpar filtros</button>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <label htmlFor="filtro-curso" className="label-mono mr-1 text-muted-foreground">Curso</label>
            <select id="filtro-curso" value={cursoId || ''} onChange={e => setParams(e.target.value ? { curso: e.target.value } : {})} className="input-base w-auto max-w-full">
              <option value="">Todos os cursos</option>
              {cursos.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <span className="label-mono mr-1 text-muted-foreground">Semestre</span>
            <button onClick={() => selecionarSemestre(null)} className={`chip ${!semestreId ? 'on' : ''}`}>Todos</button>
            {semestresVisiveis.map(s => (
              <button key={s.id} onClick={() => selecionarSemestre(s.id)} className={`chip ${semestreId === s.id ? 'on' : ''}`}>{rotuloSemestre(s)}{!cursoId && ` · ${cursos.find(c => c.id === s.cursoId)?.nome || ''}`}</button>
            ))}
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
          </div>
          <div className="flex justify-end">
            <button type="button" className="btn btn-primary" onClick={() => setFiltrosAbertos(false)}>Ver resultados</button>
          </div>
          </section>}
          </div>

          <div className="flex justify-end">
            <Link to={urlCompartilhar} className="btn btn-secondary"><Upload className="size-4" /> Compartilhar material</Link>
          </div>

          {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}

          {carregando ? (
            <p className="label-mono text-muted-foreground">Carregando materiais…</p>
          ) : filtrados.length === 0 ? (
            <div className="card-surface p-8 text-center">
              <img src={emptyMaterials} alt="Estudante pensativo diante de uma pasta vazia, representando a ausência de materiais publicados."
                className="mx-auto mb-4 h-auto w-44 max-w-full object-contain sm:w-56" />
              <p className="font-medium">{materiais.length ? 'Nenhum material encontrado' : 'Ainda não há materiais'}</p>
              <p className="mt-1 text-sm text-muted-foreground">{materiais.length ? 'Tente outro termo ou limpe os filtros.' : 'Seja o primeiro a contribuir com a turma.'}</p>
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
                  onExcluir={ehAdmin ? excluir : undefined}
                />
              ))}
            </div>
          )}
        </div>

      </div>

    </AppShell>
  )
}
