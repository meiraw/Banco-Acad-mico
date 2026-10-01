import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { AppShell } from '../components/AppShell.jsx'
import { MaterialCard } from '../components/MaterialCard.jsx'
import { VisualizadorMaterial } from '../components/VisualizadorMaterial.jsx'
import { api, rotuloSemestre } from '../lib/api.js'
import { useFavoritos } from '../lib/useFavoritos.js'

export function Favoritos() {
  const { lista } = useFavoritos()
  const [cursos, setCursos] = useState([])
  const [semestres, setSemestres] = useState([])
  const [disciplinas, setDisciplinas] = useState([])
  const [materiais, setMateriais] = useState([])
  const [previa, setPrevia] = useState(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    Promise.all([api.cursos(), api.semestres(), api.disciplinas(), api.materiais()])
      .then(([c, s, d, m]) => { setCursos(c); setSemestres(s); setDisciplinas(d); setMateriais(m) })
      .catch(e => setErro(e.message))
  }, [])

  const discPorId = useMemo(() => Object.fromEntries(disciplinas.map(d => [d.id, d])), [disciplinas])
  const favoritados = materiais.filter(m => lista.has(m.id))

  return (
    <AppShell cursos={cursos} semestres={semestres} rotuloSemestre={rotuloSemestre}>
      <h1 className="text-3xl font-bold tracking-tight">Favoritos</h1>
      <p className="mt-1 text-muted-foreground">Materiais que você marcou com o coração ficam guardados aqui, neste navegador.</p>

      {erro && <p className="mt-4 text-sm text-destructive" role="alert">{erro}</p>}

      {!favoritados.length ? (
        <div className="card-surface mt-6 p-8 text-center">
          <p className="font-medium">Nenhum favorito ainda</p>
          <p className="mt-1 text-sm text-muted-foreground">Explore o painel e clique no coração dos materiais que quiser guardar.</p>
          <Link to="/painel" className="btn btn-primary mt-4 inline-flex">Ir para o painel</Link>
        </div>
      ) : (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 2xl:grid-cols-3">
          {favoritados.map(m => (
            <MaterialCard key={m.id} material={m} disciplina={discPorId[m.disciplinaId]} cursos={cursos} semestre={semestres.find(s => s.id === discPorId[m.disciplinaId]?.semestreId)} semestreIndice={semestres.findIndex(s => s.id === discPorId[m.disciplinaId]?.semestreId)} onVisualizar={setPrevia} />
          ))}
        </div>
      )}

      <VisualizadorMaterial material={previa} disciplina={previa && discPorId[previa.disciplinaId]} onClose={() => setPrevia(null)} />
    </AppShell>
  )
}
