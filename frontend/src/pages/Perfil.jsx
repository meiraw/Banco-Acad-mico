import { useEffect, useState } from 'react'
import { AppShell } from '../components/AppShell.jsx'
import { api, rotuloSemestre } from '../lib/api.js'
import { useSessao } from '../lib/sessao.jsx'
import { iniciais } from '../lib/academico.js'

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

      <div className="card-surface mt-4 p-6">
        <p className="text-sm text-muted-foreground">
          Este login é local ao seu navegador — o backend ainda não tem cadastro/autenticação real (veja o README do projeto).
          Assim que a US03/US04 forem implementadas no servidor, esta tela passa a refletir sua conta de verdade.
        </p>
      </div>
    </AppShell>
  )
}
