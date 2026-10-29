import { useEffect, useState } from 'react'
import { AppShell } from '../components/AppShell.jsx'
import { api, rotuloSemestre } from '../lib/api.js'
import { useSessao } from '../lib/sessao.jsx'
import { iniciais } from '../lib/academico.js'
import { Administracao } from './Admin.jsx'

export function Perfil() {
  const { perfil } = useSessao()
  const [cursos, setCursos] = useState([])
  const [semestres, setSemestres] = useState([])

  useEffect(() => { Promise.all([api.cursos(), api.semestres()]).then(([c, s]) => { setCursos(c); setSemestres(s) }).catch(() => {}) }, [])

  return (
    <AppShell cursos={cursos} semestres={semestres} rotuloSemestre={rotuloSemestre}>
      <h1 className="text-3xl font-bold tracking-tight">Seu perfil</h1>

      <div className="card-surface mt-6 flex items-center gap-4 p-6">
        <span className="grid size-16 place-items-center rounded-full bg-primary text-xl font-semibold text-primary-foreground">
          {iniciais(perfil?.nome ?? 'A')}
        </span>
        <div>
          <p className="text-lg font-semibold">{perfil?.nome ?? 'Acadêmico'}</p>
          <p className="label-mono text-muted-foreground">{perfil?.email}</p>
        </div>
      </div>

      {perfil?.perfil === 'ADMIN' && (
        <Administracao onGradeAtualizada={(novosCursos, novosSemestres) => {
          setCursos(novosCursos)
          setSemestres(novosSemestres)
        }} />
      )}

    </AppShell>
  )
}
