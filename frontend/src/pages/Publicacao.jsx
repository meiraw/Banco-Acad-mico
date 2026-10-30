import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Download, Heart, MessageCircle } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { Avatar } from '../components/Avatar.jsx'
import { api, categoria, tamanho, rotuloSemestre } from '../lib/api.js'
import { useFavoritos } from '../lib/useFavoritos.js'
import { CurtirMaterial } from '../components/CurtirMaterial.jsx'
import { ComentarioPublicacao } from '../components/ComentarioPublicacao.jsx'

const data = valor => valor ? new Date(valor).toLocaleString('pt-BR') : ''

export function Publicacao() {
  const { id } = useParams()
  const [material, setMaterial] = useState(null)
  const [cursos, setCursos] = useState([])
  const [semestres, setSemestres] = useState([])
  const [disciplinas, setDisciplinas] = useState([])
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [comentarios, setComentarios] = useState([])
  const [erroComentarios, setErroComentarios] = useState('')
  const [carregandoComentarios, setCarregandoComentarios] = useState(false)
  const [pagina, setPagina] = useState(0)
  const [temMais, setTemMais] = useState(false)
  const [texto, setTexto] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [mensagem, setMensagem] = useState('')
  const envio = useRef(false)
  const consulta = useRef(0)
  const { isFavorito, alternar, disponivel } = useFavoritos()

  useEffect(() => {
    let ativo = true
    setMaterial(null); setErro(''); setCarregando(true)
    Promise.all([api.material(id), api.cursos(), api.semestres(), api.disciplinas()])
      .then(([m, c, s, d]) => { if (ativo) { setMaterial(m); setCursos(c); setSemestres(s); setDisciplinas(d) } })
      .catch(e => { if (ativo) setErro(e.message) })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [id])

  useEffect(() => {
    const versao = ++consulta.current
    setComentarios([]); setPagina(0); setTemMais(false); setTexto(''); setMensagem('')
    setErroComentarios(''); setCarregandoComentarios(true)
    api.comentarios(id).then(lista => {
      if (consulta.current === versao) { setComentarios(lista); setTemMais(lista.length === 50) }
    }).catch(e => { if (consulta.current === versao) setErroComentarios(e.message) })
      .finally(() => { if (consulta.current === versao) setCarregandoComentarios(false) })
    return () => { consulta.current++ }
  }, [id])

  async function carregarComentarios(proxima = 0) {
    const versao = consulta.current
    setCarregandoComentarios(true); setErroComentarios('')
    try {
      const lista = await api.comentarios(id, proxima)
      if (consulta.current !== versao) return
      setComentarios(anteriores => proxima === 0 ? lista : [...anteriores, ...lista.filter(c => !anteriores.some(a => a.id === c.id))])
      setPagina(proxima); setTemMais(lista.length === 50)
    } catch (e) { if (consulta.current === versao) setErroComentarios(e.message) }
    finally { if (consulta.current === versao) setCarregandoComentarios(false) }
  }

  async function comentar(event) {
    event.preventDefault()
    if (!texto.trim() || envio.current) return
    const versao = consulta.current
    envio.current = true; setEnviando(true); setErroComentarios(''); setMensagem('')
    try {
      await api.comentar(id, texto.trim())
      if (consulta.current !== versao) return
      setTexto(''); setMensagem('Comentário publicado.')
      await carregarComentarios(0)
    } catch (e) { if (consulta.current === versao) setErroComentarios(e.message) }
    finally { envio.current = false; setEnviando(false) }
  }

  const disciplina = disciplinas.find(d => d.id === material?.disciplinaId)
  const semestre = semestres.find(s => s.id === disciplina?.semestreId)
  const curso = cursos.find(c => c.id === semestre?.cursoId)
  const url = material && api.urlArquivo(material.id)
  const cat = material && categoria(material.tipoArquivo)
  const imagemSegura = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(material?.tipoArquivo)

  return <AppShell cursos={cursos} semestres={semestres} rotuloSemestre={rotuloSemestre}>
    <Link to="/painel" className="inline-flex items-center gap-2 text-sm text-secondary"><ArrowLeft className="size-4" /> Voltar aos materiais</Link>
    {carregando && <p role="status" className="mt-6 text-muted-foreground">Carregando publicação…</p>}
    {erro && <p role="alert" className="mt-6 text-destructive">{erro}</p>}
    {material && <div className="mx-auto mt-6 max-w-5xl space-y-6">
      <article className="card-surface overflow-hidden">
        <header className="space-y-4 p-5 sm:p-8">
          <p className="label-mono text-secondary">{[curso?.nome, semestre && rotuloSemestre(semestre), disciplina?.nome].filter(Boolean).join(' · ')}</p>
          <h1 className="break-words text-3xl font-bold tracking-tight sm:text-4xl">{material.titulo}</h1>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <Avatar nome={material.autorNome} className="size-10" />
              <div className="min-w-0"><p className="text-xs text-muted-foreground">Publicado por</p>
                <p className="break-words text-base font-semibold">{material.autorNome || 'Autor não informado'}</p>
                {material.publicadoEm && <time dateTime={material.publicadoEm} className="text-xs text-muted-foreground">{data(material.publicadoEm)}</time>}
              </div>
            </div>
            <div className="flex flex-wrap items-start gap-3">
            <CurtirMaterial key={id} materialId={id} />
            <button type="button" disabled={!disponivel} aria-pressed={isFavorito(id)} onClick={() => alternar(id)} className={`btn btn-outline ${isFavorito(id) ? 'text-destructive' : ''}`}>
              <Heart className={`size-4 ${isFavorito(id) ? 'fill-current' : ''}`} /> {isFavorito(id) ? 'Remover dos favoritos' : 'Favoritar'}
            </button>
            </div>
          </div>
          <p className="text-xs text-muted-foreground">Os favoritos ficam guardados apenas para sua conta neste navegador.</p>
          <p className="whitespace-pre-wrap break-words text-muted-foreground">{material.descricao}</p>
        </header>
        <div className="border-y border-border bg-surface-container">
          {cat === 'PDF' ? <iframe src={url} title={material.titulo} className="h-[70vh] min-h-80 w-full" />
            : imagemSegura ? <img src={url} alt={material.titulo} className="mx-auto max-h-[70vh] max-w-full object-contain" />
              : <p className="p-10 text-center text-muted-foreground">A prévia deste formato não está disponível. Baixe o arquivo para ver o conteúdo.</p>}
        </div>
        <footer className="flex flex-wrap items-center justify-between gap-4 p-5">
          <p className="min-w-0 break-all text-sm text-muted-foreground">{material.nomeArquivo} · {tamanho(material.tamanhoArquivo)}</p>
          <a href={`${url}?download=true`} download={material.nomeArquivo} className="btn btn-primary"><Download className="size-4" /> Baixar material</a>
        </footer>
      </article>

      <section aria-labelledby="titulo-comentarios" className="card-surface space-y-6 p-5 sm:p-8">
        <h2 id="titulo-comentarios" className="flex items-center gap-2 text-xl font-semibold"><MessageCircle className="size-5" /> Comentários</h2>
        <form onSubmit={comentar} className="space-y-3">
          <label htmlFor="comentario" className="block text-sm font-medium">Deixe seu comentário</label>
          <textarea id="comentario" value={texto} onChange={e => setTexto(e.target.value)} required maxLength={2000} rows={3} disabled={enviando} className="input-base h-auto resize-y py-2" placeholder="Compartilhe uma dúvida ou contribuição sobre o material" />
          <div className="flex items-center justify-between gap-3"><span className="text-xs text-muted-foreground">{texto.length}/2000</span>
            <button type="submit" disabled={enviando || !texto.trim()} className="btn btn-primary">{enviando ? 'Publicando…' : 'Publicar comentário'}</button></div>
        </form>
        {mensagem && <p role="status" className="text-sm text-secondary">{mensagem}</p>}
        {erroComentarios && <div role="alert" className="text-sm text-destructive">{erroComentarios} <button type="button" disabled={carregandoComentarios || enviando} onClick={() => carregarComentarios(0)} className="underline">Tentar novamente</button></div>}
        {comentarios.map(c => <ComentarioPublicacao key={`${id}-${c.id}`} materialId={id} comentario={c} />)}
        {carregandoComentarios ? <p role="status" className="text-sm text-muted-foreground">Carregando comentários…</p>
          : !comentarios.length && !erroComentarios && <p className="text-sm text-muted-foreground">Ainda não há comentários. Seja o primeiro a participar.</p>}
        {temMais && <button type="button" onClick={() => carregarComentarios(pagina + 1)} disabled={carregandoComentarios || enviando} className="btn btn-outline">Ver mais comentários</button>}
      </section>
    </div>}
  </AppShell>
}
