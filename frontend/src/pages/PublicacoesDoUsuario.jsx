import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { AppShell } from '../components/AppShell.jsx'
import { MinhasPublicacoes } from '../components/MinhasPublicacoes.jsx'
import { api } from '../lib/api.js'

export function PublicacoesDoUsuario() {
  const { state } = useLocation()
  const [cursos, setCursos] = useState([])
  const [semestres, setSemestres] = useState([])
  const [erro, setErro] = useState('')
  const [tentativa, setTentativa] = useState(0)
  useEffect(() => {
    let ativo = true
    setErro('')
    Promise.all([api.cursos(), api.semestres()])
      .then(([c, s]) => { if (ativo) { setCursos(c); setSemestres(s) } })
      .catch(e => { if (ativo) setErro(e.message) })
    return () => { ativo = false }
  }, [tentativa])
  return <AppShell>
    {state?.materialEnviado && <p role="status" className="mb-4 text-secondary">Material enviado com sucesso! Sua publicação está aguardando aprovação de um administrador.</p>}
    {erro && <div role="alert" className="card-surface mb-4 p-4 text-sm text-destructive">{erro} <button type="button" className="underline" onClick={() => setTentativa(t => t + 1)}>Recarregar cursos e semestres</button></div>}
    <MinhasPublicacoes principal cursos={cursos} semestres={semestres} />
  </AppShell>
}
