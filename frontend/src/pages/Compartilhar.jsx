import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { UploadCloud, ShieldAlert } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { api, rotuloSemestre } from '../lib/api.js'

export function Compartilhar() {
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
  const [protegido, setProtegido] = useState(false)
  const [autoral, setAutoral] = useState(false)
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
    if (arquivo.size > 20 * 1024 * 1024) { setErro('O arquivo deve ter até 20 MB.'); return }
    if (protegido && referencia.trim().length < 10) { setErro('Como o material é protegido por direitos autorais, a referência bibliográfica é obrigatória.'); return }
    if (!autoral) { setErro('Confirme que você é responsável pelo conteúdo enviado.'); return }

    setEnviando(true)
    try {
      await api.criarMaterial({ titulo: titulo.trim(), descricao: descricao.trim(), disciplinaId }, arquivo)
      navigate(`/painel?curso=${cursoId}&semestre=${semestreId}`, { replace: true })
    } catch (err) { setErro(err.message); setEnviando(false) }
  }

  return (
    <AppShell cursos={cursos} semestres={semestresVisiveis} rotuloSemestre={rotuloSemestre}>
      <div className="mx-auto max-w-2xl">
        <h1 className="text-3xl font-bold tracking-tight">Compartilhar material</h1>
        <p className="mt-1 text-muted-foreground">Ajude seus colegas enviando resumos, listas, slides e anotações.</p>

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
            <label htmlFor="arquivo" className="label-mono block">Arquivo (até 20 MB)</label>
            <label htmlFor="arquivo" className="flex cursor-pointer flex-col items-center gap-2 rounded-lg border-2 border-dashed border-border bg-surface-container px-4 py-8 text-center">
              <UploadCloud className="size-8 text-primary" />
              <span className="label-mono text-muted-foreground">{arquivo ? arquivo.name : 'Clique para selecionar o arquivo'}</span>
            </label>
            <input id="arquivo" type="file" className="sr-only" accept=".pdf,.ppt,.pptx,.doc,.docx,image/*" onChange={e => setArquivo(e.target.files?.[0] ?? null)} />
          </div>

          <div className="space-y-3 rounded-lg border-2 border-tertiary/50 bg-tertiary/10 p-4">
            <div className="flex items-start gap-2">
              <ShieldAlert className="mt-0.5 size-5 shrink-0 text-tertiary" />
              <p className="text-sm">
                <strong>Direitos autorais e dados pessoais:</strong> se o material é protegido por direitos autorais, informe a referência bibliográfica. Antes de enviar provas ou trabalhos, rasure CPF, telefone, matrícula e notas pessoais.
              </p>
            </div>
            <label className="label-mono flex items-center gap-2">
              <input type="checkbox" checked={protegido} onChange={e => setProtegido(e.target.checked)} className="size-4 accent-primary" />
              Este material é protegido por direitos autorais
            </label>
            <div className="space-y-1.5">
              <label htmlFor="referencia" className="label-mono block">Referência bibliográfica {protegido ? '(obrigatória)' : '(opcional)'}</label>
              <textarea id="referencia" value={referencia} onChange={e => setReferencia(e.target.value)} maxLength={500} rows={2} required={protegido}
                placeholder="CORMEN, T. H. et al. Algoritmos: teoria e prática. 3. ed. Rio de Janeiro: Elsevier, 2012."
                className="input-base h-auto py-2" />
            </div>
          </div>

          <label className="label-mono flex items-start gap-2">
            <input type="checkbox" checked={autoral} onChange={e => setAutoral(e.target.checked)} className="mt-0.5 size-4 accent-primary" />
            Sou responsável pelo conteúdo enviado e informo que ele não viola direitos de terceiros (Marco Civil da Internet).
          </label>

          {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}

          <button type="submit" disabled={enviando} className="btn btn-primary w-full">{enviando ? 'Enviando…' : 'Publicar material'}</button>
        </form>
      </div>
    </AppShell>
  )
}
