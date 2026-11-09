import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Star, Settings, LogOut, ShieldCheck } from 'lucide-react'
import { useSessao } from '../lib/sessao.jsx'
import { Avatar } from './Avatar.jsx'
import { Logo } from './Logo.jsx'

export function AppShell({ children }) {
  const { perfil, fotoUrl, sair } = useSessao()
  const navigate = useNavigate()
  const location = useLocation()

  function sairEVoltar() {
    sair()
    navigate('/auth', { replace: true })
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-30 border-b border-border bg-card">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4">
          <Link to="/painel" className="min-w-0">
            <Logo className="w-36 sm:w-44" />
          </Link>
        </div>
      </header>

      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-6 lg:flex-row lg:gap-6">
        <aside className="w-full shrink-0 lg:w-56 lg:self-stretch">
          {/* A coluna reserva o espaço; perfil e navegação ficam fixos somente no desktop. */}
          <div className="space-y-4 lg:fixed lg:top-24 lg:w-56">
          <Link to="/perfil" className="flex min-w-0 flex-col items-start gap-3 px-3 text-left">
            <div className="flex w-full min-w-0 items-center gap-3">
              <Avatar nome={perfil?.nome} fotoUrl={fotoUrl} className="size-14 text-xl" />
              <p className="break-words text-lg font-semibold leading-tight">{perfil?.nome ?? 'Acadêmico'}</p>
            </div>
            <div className="w-full min-w-0 space-y-1">
              <p className="text-sm text-muted-foreground">{perfil?.semestreAtual ? `${perfil.semestreAtual}º semestre` : 'Semestre não informado'}</p>
              <p className="break-words text-sm text-muted-foreground">{perfil?.curso || 'Curso não informado'}</p>
            </div>
          </Link>
          <nav aria-label="Navegação principal" className="flex flex-wrap gap-1 lg:flex-col">
          <Link to="/favoritos" className="label-mono flex items-center gap-3 rounded-lg px-3 py-2 text-muted-foreground transition-colors hover:bg-surface-container hover:text-primary">
            <Star className="size-4" /> Favoritos
          </Link>
          <Link to="/configuracoes" className="label-mono flex items-center gap-3 rounded-lg px-3 py-2 text-muted-foreground transition-colors hover:bg-surface-container hover:text-primary">
            <Settings className="size-4" /> Configurações
          </Link>
          <button type="button" onClick={sairEVoltar} className="label-mono flex items-center gap-3 rounded-lg px-3 py-2 text-left text-muted-foreground transition-colors hover:bg-surface-container hover:text-primary">
            <LogOut className="size-4" /> Sair da conta
          </button>
          </nav>
          </div>
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
