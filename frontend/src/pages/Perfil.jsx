import { useEffect, useState } from 'react'
import { AppShell } from '../components/AppShell.jsx'
import { api, rotuloSemestre } from '../lib/api.js'
import { useSessao } from '../lib/sessao.jsx'
import { Administracao } from './Admin.jsx'
import { MinhasPublicacoes } from '../components/MinhasPublicacoes.jsx'
import { useLocation } from 'react-router-dom'

export function Perfil() {
  const { state } = useLocation()
  const { perfil } = useSessao()
  const [cursos, setCursos] = useState([])
  const [semestres, setSemestres] = useState([])

  useEffect(() => { Promise.all([api.cursos(), api.semestres()]).then(([c, s]) => { setCursos(c); setSemestres(s) }).catch(() => {}) }, [])

  return (
    <AppShell cursos={cursos} semestres={semestres} rotuloSemestre={rotuloSemestre}>
      <h1 className="text-3xl font-bold tracking-tight">Publicações e administração</h1>
      {state?.materialEnviado && <p role="status" className="mt-4 text-secondary">Material enviado com sucesso! Sua publicação está aguardando aprovação de um administrador.</p>}

      <MinhasPublicacoes cursos={cursos} semestres={semestres} />

      {perfil?.perfil === 'ADMIN' && (
        <Administracao onGradeAtualizada={(novosCursos, novosSemestres) => {
          setCursos(novosCursos)
          setSemestres(novosSemestres)
        }} />
      )}

    </AppShell>
  )
}
