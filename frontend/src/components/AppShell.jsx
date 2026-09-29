import { Link, useLocation, useNavigate } from 'react-router-dom'
import { GraduationCap, Star, Info, FileText, LogOut, Upload, ShieldCheck } from 'lucide-react'
import { useSessao } from '../lib/sessao.jsx'
import { iniciais, corSemestre } from '../lib/academico.js'
import { ThemeToggle } from './ThemeToggle.jsx'

export function AppShell({ semestres = [], rotuloSemestre, children }) {
  const { perfil, sair } = useSessao()
  const navigate = useNavigate()
  const location = useLocation()

  function sairEVoltar() {
    sair()
    navigate('/auth', { replace: true })
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-30 border-b border-border bg-card/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4">
          <Link to="/painel" className="flex items-center gap-2 text-primary">
            <GraduationCap className="size-6" />
            <span className="text-lg font-bold tracking-tight">Banco Acadêmico</span>
          </Link>
          <div className="flex-1" />
          <ThemeToggle />
          <Link to="/perfil" className="flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-semibold leading-tight">{perfil?.nome ?? 'Acadêmico'}</p>
              <p className="label-mono text-secondary">{perfil?.email}</p>
            </div>
            <span className="grid size-10 place-items-center rounded-full bg-primary font-semibold text-primary-foreground">
              {iniciais(perfil?.nome ?? 'A')}
            </span>
          </Link>
          <button className="btn btn-ghost btn-icon" aria-label="Sair" onClick={sairEVoltar}><LogOut className="size-5" /></button>
        </div>
      </header>

      <div className="mx-auto flex max-w-7xl gap-6 px-4 py-6">
        <aside className="sticky top-22 hidden h-fit w-56 shrink-0 lg:block">
          <div className="mb-4 flex items-center gap-2 px-2">
            <GraduationCap className="size-5 text-primary" />
            <div>
              <p className="font-semibold text-primary">Semestres</p>
              <p className="label-mono text-muted-foreground">Ciência da Computação</p>
            </div>
          </div>
          <nav className="space-y-1">
            {semestres.map((s, i) => (
              <Link
                key={s.id}
                to={`/painel?semestre=${s.id}`}
                className="label-mono flex items-center gap-3 rounded-lg px-3 py-2 text-muted-foreground transition-colors hover:bg-surface-container hover:text-primary"
              >
                <span className="grid size-6 shrink-0 place-items-center rounded font-semibold text-neutral-900" style={{ backgroundColor: corSemestre(i) }}>
                  {i + 1}
                </span>
                {rotuloSemestre(s)}
              </Link>
            ))}
          </nav>
          <div className="my-4 h-px bg-border" />
          <Link to="/favoritos" className="label-mono flex items-center gap-3 rounded-lg px-3 py-2 text-muted-foreground transition-colors hover:bg-surface-container hover:text-primary">
            <Star className="size-4" /> Favoritos
          </Link>
          <Link to="/compartilhar" className="label-mono flex items-center gap-3 rounded-lg px-3 py-2 text-muted-foreground transition-colors hover:bg-surface-container hover:text-primary">
            <Upload className="size-4" /> Compartilhar
          </Link>
          <div className="my-4 h-px bg-border" />
          <Link to="/sobre" className="label-mono flex items-center gap-3 rounded-lg px-3 py-2 text-muted-foreground transition-colors hover:bg-surface-container hover:text-primary">
            <Info className="size-4" /> Sobre
          </Link>
          <Link to="/termos" className="label-mono flex items-center gap-3 rounded-lg px-3 py-2 text-muted-foreground transition-colors hover:bg-surface-container hover:text-primary">
            <FileText className="size-4" /> Termos
          </Link>
        </aside>

        <main className="min-w-0 flex-1" key={location.pathname + location.search}>
          {children}
        </main>
      </div>

      <footer className="border-t border-border bg-card">
        <div className="label-mono mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-6 text-muted-foreground">
          <span>© {new Date().getFullYear()} Banco Acadêmico · UNEMAT</span>
          <span className="flex items-center gap-1"><ShieldCheck className="size-3.5" /> Apoio institucional a estudantes de Computação</span>
        </div>
      </footer>
    </div>
  )
}
