import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { AppShell } from '../components/AppShell.jsx'
import { MaterialCard } from '../components/MaterialCard.jsx'
import { api, rotuloSemestre } from '../lib/api.js'
import { useFavoritos } from '../lib/useFavoritos.js'
import { useSessao } from '../lib/sessao.jsx'

export function Favoritos() {
  const { perfil } = useSessao()
  const conta = perfil?.id
  const { disponivel, revisao, erro: erroEstado, recarregar } = useFavoritos()
  const [cursos, setCursos] = useState([])
  const [semestres, setSemestres] = useState([])
  const [disciplinas, setDisciplinas] = useState([])
  const [resultado, setResultado] = useState(null)
  const [pagina, setPagina] = useState(0)
  const [carregando, setCarregando] = useState(true)
  const [tentativa, setTentativa] = useState(0)
  const [erro, setErro] = useState('')
  const [erroCatalogo, setErroCatalogo] = useState('')

  useEffect(() => {
    let ativo = true
    setErroCatalogo('')
    Promise.all([api.cursos(), api.semestres(), api.disciplinas()])
      .then(([c, s, d]) => { if (ativo) { setCursos(c); setSemestres(s); setDisciplinas(d) } })
      .catch(e => { if (ativo) setErroCatalogo(e.message) })
    return () => { ativo = false }
  }, [conta, tentativa])

  useEffect(() => {
    if (!disponivel) return
    let ativo = true
    setCarregando(true); setErro('')
    api.favoritos(pagina).then(dados => {
      if (!ativo) return
      if (!dados.content.length && pagina > 0) setPagina(p => p - 1)
      else setResultado({ conta, pagina, dados })
    }).catch(e => { if (ativo) setErro(e.message) })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [conta, disponivel, pagina, revisao, tentativa])

  const discPorId = useMemo(() => Object.fromEntries(disciplinas.map(d => [d.id, d])), [disciplinas])
  const dados = resultado?.conta === conta && resultado?.pagina === pagina ? resultado.dados : null
  const favoritados = dados?.content || []
  const erroAtual = erro || erroCatalogo || erroEstado

  return (
    <AppShell cursos={cursos} semestres={semestres} rotuloSemestre={rotuloSemestre}>
      <h1 className="text-3xl font-bold tracking-tight">Favoritos</h1>
      <p className="mt-1 text-muted-foreground">Materiais que você marcou com o coração ficam salvos na sua conta, disponíveis em outros navegadores e dispositivos.</p>

      {erroAtual && <p className="mt-4 text-sm text-destructive" role="alert">{erroAtual} <button type="button" className="underline" onClick={() => { setTentativa(t => t + 1); void recarregar() }}>Tentar novamente</button></p>}

      {!disponivel || carregando || (!dados && !erro) ? (!erroEstado && <p role="status" className="mt-6 text-sm text-muted-foreground">Carregando seus favoritos…</p>) : erro && !dados ? null : !favoritados.length ? (
        <div className="card-surface mt-6 p-8 text-center">
          <p className="font-medium">Nenhum favorito ainda</p>
          <p className="mt-1 text-sm text-muted-foreground">Explore o painel e clique no coração dos materiais que quiser guardar.</p>
          <Link to="/painel" className="btn btn-primary mt-4 inline-flex">Ir para o painel</Link>
        </div>
      ) : (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 2xl:grid-cols-3">
          {favoritados.map(m => (
            <MaterialCard key={m.id} material={m} disciplina={discPorId[m.disciplinaId]} cursos={cursos} semestre={semestres.find(s => s.id === discPorId[m.disciplinaId]?.semestreId)} semestreIndice={semestres.findIndex(s => s.id === discPorId[m.disciplinaId]?.semestreId)} />
          ))}
        </div>
      )}
      {dados && (pagina > 0 || !dados.last) && <nav aria-label="Páginas dos favoritos" className="mt-6 flex items-center justify-between gap-3">
        <button type="button" className="btn btn-outline" disabled={pagina === 0 || carregando} onClick={() => setPagina(p => p - 1)}>Anterior</button>
        <span className="text-sm text-muted-foreground">Página {pagina + 1}</span>
        <button type="button" className="btn btn-outline" disabled={dados.last || carregando} onClick={() => setPagina(p => p + 1)}>Próxima</button>
      </nav>}

    </AppShell>
  )
}
