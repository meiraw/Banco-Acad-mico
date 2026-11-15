import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { UploadCloud, ShieldAlert } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { api, rotuloSemestre } from '../lib/api.js'
import { TIPOS_PUBLICACAO, dadosPublicacao, permiteReferencia, rotuloTipoPublicacao } from '../lib/tipoPublicacao.js'
import { useSessao } from '../lib/sessao.jsx'

export function Compartilhar() {
  const { perfil } = useSessao()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [cursoId, setCursoId] = useState(params.get('curso') || '')
  const [semestreId, setSemestreId] = useState(params.get('semestre') || '')
  const [cursos, setCursos] = useState([])
  const [semestres, setSemestres] = useState([])
  const [disciplinas, setDisciplinas] = useState([])
  const [titulo, setTitulo] = useState('')
  const [descricao, setDescricao] = useState('')
  const [disciplinaId, setDisciplinaId] = useState('')
  const [arquivo, setArquivo] = useState(null)
  const [referencia, setReferencia] = useState('')
  const [tipoPublicacao, setTipoPublicacao] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    Promise.all([api.cursos(), api.semestres(), api.disciplinas()]).then(([c, s, d]) => {
      setCursos(c); setSemestres(s)
      const inicial = s.find(semestre => semestre.id === params.get('semestre'))
      if (!params.get('curso') && inicial) setCursoId(inicial.cursoId)
      setDisciplinas(d)
    }).catch(e => setErro(e.message))
  }, [])

  const semestresVisiveis = semestres.filter(s => s.cursoId === cursoId)
  const disciplinasVisiveis = disciplinas.filter(d => d.semestreId === semestreId && semestresVisiveis.some(s => s.id === d.semestreId))

  async function enviar(e) {
    e.preventDefault()
    setErro('')
    if (titulo.trim().length < 2) { setErro('Informe um título com pelo menos 2 caracteres.'); return }
    if (descricao.trim().length < 2) { setErro('Informe uma descrição com pelo menos 2 caracteres.'); return }
    if (!disciplinasVisiveis.some(d => d.id === disciplinaId)) { setErro('Selecione a disciplina.'); return }
    if (!arquivo) { setErro('Selecione o arquivo do material.'); return }
    if (!arquivo.size) { setErro('Selecione um arquivo não vazio.'); return }
    if (!/\.(pdf|doc|docx|ppt|pptx|jpe?g|png|gif)$/i.test(arquivo.name)) { setErro('Use PDF, DOC, DOCX, PPT, PPTX, JPG, PNG ou GIF.'); return }
    if (arquivo.size > 20 * 1024 * 1024) { setErro('O arquivo deve ter até 20 MB.'); return }
    if (!TIPOS_PUBLICACAO.some(([valor]) => valor === tipoPublicacao)) { setErro('Selecione o tipo de publicação.'); return }

    setEnviando(true)
    try {
      const material = await api.criarMaterial({ titulo: titulo.trim(), descricao: descricao.trim(), disciplinaId, ...dadosPublicacao(tipoPublicacao, referencia) }, arquivo)
      navigate(perfil?.perfil === 'ADMIN' ? '/perfil' : '/minhas-publicacoes', { replace: true, state: { materialEnviado: material.id } })
    } catch (err) { setErro(err.message); setEnviando(false) }
  }

  return (
    <AppShell cursos={cursos} semestres={semestresVisiveis} rotuloSemestre={rotuloSemestre}>
      <div className="mx-auto max-w-2xl">
        <h1 className="text-3xl font-bold tracking-tight">Compartilhar material</h1>
        <p className="mt-1 text-muted-foreground">Ajude seus colegas enviando resumos, listas, slides e anotações.</p>
        <p className="mt-3 text-sm text-muted-foreground">Compartilhe somente materiais de sua autoria ou conteúdos cuja publicação seja permitida por autorização, licença ou outra hipótese legal aplicável.</p>

        <form onSubmit={enviar} className="card-surface mt-6 space-y-4 p-6">
          <div className="space-y-1.5">
            <label htmlFor="titulo" className="label-mono block">Título</label>
            <input id="titulo" value={titulo} onChange={e => setTitulo(e.target.value)} maxLength={100} required
              placeholder="Resumo de Estruturas de Dados — Listas encadeadas" className="input-base" />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="descricao" className="label-mono block">Descrição</label>
            <textarea id="descricao" value={descricao} onChange={e => setDescricao(e.target.value)} maxLength={100} rows={3} required
              placeholder="Conte o que o material cobre e como ele pode ajudar." className="input-base h-auto py-2" />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="curso" className="label-mono block">Curso</label>
            <select id="curso" required value={cursoId} onChange={e => { setCursoId(e.target.value); setSemestreId(''); setDisciplinaId('') }} className="input-base">
              <option value="">Selecione…</option>
              {cursos.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
            {!cursos.length && <p className="text-sm text-muted-foreground">Cadastre um curso no painel antes de enviar.</p>}
          </div>
          <div className="space-y-1.5">
            <label htmlFor="semestre" className="label-mono block">Semestre</label>
            <select id="semestre" required disabled={!cursoId} value={semestreId} onChange={e => { setSemestreId(e.target.value); setDisciplinaId('') }} className="input-base">
              <option value="">Selecione…</option>
              {semestresVisiveis.map(s => <option key={s.id} value={s.id}>{rotuloSemestre(s)}</option>)}
            </select>
            {cursoId && !semestresVisiveis.length && <p className="text-sm text-muted-foreground">Este curso ainda não tem semestres. Cadastre um no painel.</p>}
          </div>
          <div className="space-y-1.5">
            <label htmlFor="disciplina" className="label-mono block">Disciplina</label>
            <select id="disciplina" disabled={!semestreId} value={disciplinaId} onChange={e => setDisciplinaId(e.target.value)} required className="input-base">
              <option value="">Selecione…</option>
              {disciplinasVisiveis.map(d => <option key={d.id} value={d.id}>{d.nome}</option>)}
            </select>
            {semestreId && !disciplinasVisiveis.length && <p className="text-sm text-muted-foreground">Nenhuma disciplina cadastrada ainda — crie uma no painel antes de enviar.</p>}
          </div>

          <div className="space-y-1.5">
            <label htmlFor="tipo-publicacao" className="label-mono block">Tipo de Publicação</label>
            <select id="tipo-publicacao" required value={tipoPublicacao} onChange={e => { setTipoPublicacao(e.target.value); setReferencia('') }} className="input-base">
              <option value="">Selecione…</option>
              {TIPOS_PUBLICACAO.map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}
            </select>
          </div>

          {permiteReferencia(tipoPublicacao) && <div className="space-y-1.5">
            <label htmlFor="referencia" className="label-mono block">Referência Bibliográfica do {rotuloTipoPublicacao(tipoPublicacao)}</label>
            <textarea id="referencia" value={referencia} onChange={e => setReferencia(e.target.value)} maxLength={500} rows={2} required={tipoPublicacao === 'ARTIGO'}
              aria-describedby="orientacao-referencia" placeholder="Autor, título, edição, local, editora e ano."
              className="input-base h-auto py-2" />
            <p id="orientacao-referencia" className="text-sm text-muted-foreground">{tipoPublicacao === 'ARTIGO' ? 'Obrigatória: informe a referência bibliográfica do artigo.' : 'Opcional: informe a referência bibliográfica do livro, quando aplicável.'}</p>
          </div>}

          <div className="space-y-1.5">
            <label htmlFor="arquivo" className="label-mono block">Arquivo (até 20 MB)</label>
            <label htmlFor="arquivo" className="flex cursor-pointer flex-col items-center gap-2 rounded-lg border-2 border-dashed border-border bg-surface-container px-4 py-8 text-center">
              <UploadCloud className="size-8 text-primary" />
              <span className="label-mono text-muted-foreground">{arquivo ? arquivo.name : 'Clique para selecionar o arquivo'}</span>
            </label>
            <input id="arquivo" type="file" className="sr-only" accept=".pdf,.ppt,.pptx,.doc,.docx,.jpg,.jpeg,.png,.gif" onChange={e => setArquivo(e.target.files?.[0] ?? null)} />
            <p className="text-sm text-muted-foreground">PDF, DOC, DOCX, PPT, PPTX, JPG ou PNG.</p>
          </div>

          <div className="space-y-3 rounded-lg border-2 border-tertiary/50 bg-tertiary/10 p-4">
            <div className="flex items-start gap-2">
              <ShieldAlert className="mt-0.5 size-5 shrink-0 text-tertiary" />
              <p className="text-sm">
                <strong>Publicação e moderação:</strong> revise as informações e o arquivo antes de enviar. O conteúdo será analisado pela moderação e ficará disponível para os demais usuários após a aprovação. Você pode acompanhar o status em Minhas publicações.
              </p>
            </div>
          </div>

          {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}

          <button type="submit" disabled={enviando} className="btn btn-primary w-full">{enviando ? 'Enviando…' : 'Publicar material'}</button>
        </form>
      </div>
    </AppShell>
  )
}
