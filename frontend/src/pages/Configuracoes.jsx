import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, FileText, Info, Moon } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { api, rotuloSemestre } from '../lib/api.js'
import { useTema } from '../lib/useTema.js'

export function Configuracoes() {
  const { tema, alternar, disponivel } = useTema()
  const escuro = tema === 'escuro'
  const [cursos, setCursos] = useState([])
  const [semestres, setSemestres] = useState([])

  useEffect(() => {
    let ativo = true
    Promise.all([api.cursos(), api.semestres()]).then(([c, s]) => {
      if (ativo) { setCursos(c); setSemestres(s) }
    }).catch(() => {})
    return () => { ativo = false }
  }, [])

  return <AppShell cursos={cursos} semestres={semestres} rotuloSemestre={rotuloSemestre}>
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Configurações</h1>
        <p className="mt-1 text-muted-foreground">Personalize a aparência e consulte as informações do site.</p>
      </div>

      <section aria-labelledby="aparencia" className="card-surface p-5 sm:p-6">
        <h2 id="aparencia" className="text-lg font-semibold">Aparência</h2>
        <div className="mt-5 flex items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <Moon className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden="true" />
            <div>
              <p id="modo-noturno" className="font-medium">Modo noturno</p>
              <p id="descricao-tema" className="mt-1 text-sm text-muted-foreground">{escuro ? 'Tema escuro ativado.' : 'Tema claro ativado.'} Sua preferência fica salva apenas para sua conta neste navegador.</p>
            </div>
          </div>
          <button type="button" role="switch" aria-checked={escuro} aria-labelledby="modo-noturno" aria-describedby="descricao-tema" onClick={alternar} disabled={!disponivel}
            className={`inline-flex h-7 w-12 shrink-0 items-center rounded-full p-1 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${escuro ? 'bg-primary' : 'bg-muted-foreground'}`}>
            <span aria-hidden="true" className={`size-5 rounded-full bg-card shadow-sm transition-transform ${escuro ? 'translate-x-5' : 'translate-x-0'}`} />
          </button>
        </div>
      </section>

      <section aria-labelledby="informacoes" className="card-surface p-5 sm:p-6">
        <h2 id="informacoes" className="text-lg font-semibold">Informações</h2>
        <div className="mt-4 divide-y divide-border">
          <Link to="/sobre" className="flex items-center gap-3 rounded-lg py-4 transition-colors hover:bg-surface-container">
            <Info className="size-5 shrink-0 text-primary" aria-hidden="true" />
            <div className="min-w-0 flex-1"><p className="font-medium">Sobre</p><p className="mt-1 text-sm text-muted-foreground">Conheça o Banco Acadêmico e seu objetivo.</p></div>
            <ArrowRight className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          </Link>
          <Link to="/termos" className="flex items-center gap-3 rounded-lg py-4 transition-colors hover:bg-surface-container">
            <FileText className="size-5 shrink-0 text-primary" aria-hidden="true" />
            <div className="min-w-0 flex-1"><p className="font-medium">Termos de uso</p><p className="mt-1 text-sm text-muted-foreground">Consulte as regras de uso e compartilhamento.</p></div>
            <ArrowRight className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          </Link>
        </div>
      </section>
    </div>
  </AppShell>
}
