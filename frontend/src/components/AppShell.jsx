import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { Star, Settings, LogOut, ShieldCheck, House, MessagesSquare, Files } from 'lucide-react'
import { useSessao } from '../lib/sessao.jsx'
import { Avatar } from './Avatar.jsx'
import { Logo } from './Logo.jsx'
import { BotaoVoltar } from './BotaoVoltar.jsx'

export function AppShell({ children }) {
  const { perfil, fotoUrl, sair } = useSessao()
  const navigate = useNavigate()
  const location = useLocation()
  const ehAdmin = perfil?.perfil === 'ADMIN'
  const Identificacao = ehAdmin ? Link : 'div'
  const itemMenu = ({ isActive }) => `label-mono flex items-center gap-3 rounded-lg px-3 py-2 transition-colors ${isActive ? 'bg-surface-container text-primary font-semibold' : 'text-muted-foreground hover:bg-surface-container hover:text-primary'}`

  function sairEVoltar() {
    sair()
    navigate('/auth', { replace: true })
  }

  return (
    <div className="flex min-h-screen flex-col bg-background supports-[min-height:100dvh]:min-h-dvh">
      <header className="sticky top-0 z-30 shrink-0 border-b border-border bg-card">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4">
          <Link to="/painel" className="min-w-0">
            <Logo className="w-36 sm:w-44" />
          </Link>
        </div>
      </header>

      <div className="mx-auto flex w-full max-w-7xl flex-1 flex-col gap-4 px-4 py-6 lg:flex-row lg:gap-6">
        <aside className="w-full shrink-0 lg:w-56 lg:self-stretch">
          {/* A coluna reserva o espaço; perfil e navegação ficam fixos somente no desktop. */}
          <div className="space-y-4 lg:fixed lg:top-24 lg:w-56">
          <Identificacao {...(ehAdmin ? { to: '/perfil' } : {})} className="flex min-w-0 flex-col items-start gap-3 px-3 text-left">
            <div className="flex w-full min-w-0 items-center gap-3">
              <Avatar nome={perfil?.nome} fotoUrl={fotoUrl} className="size-14 text-xl" />
              <p className="break-words text-lg font-semibold leading-tight">{perfil?.nome ?? 'Acadêmico'}</p>
            </div>
            <div className="w-full min-w-0 space-y-1">
              <p className="text-sm text-muted-foreground">{perfil?.semestreAtual ? `${perfil.semestreAtual}º semestre` : 'Semestre não informado'}</p>
              <p className="break-words text-sm text-muted-foreground">{perfil?.curso || 'Curso não informado'}</p>
            </div>
          </Identificacao>
          <nav aria-label="Navegação principal" className="flex flex-wrap gap-1 lg:flex-col">
          <NavLink to="/painel" className={itemMenu}>
            <House className="size-4" aria-hidden="true" /> Início
          </NavLink>
          <NavLink to="/forum" className={itemMenu}>
            <MessagesSquare className="size-4" aria-hidden="true" /> Fórum
          </NavLink>
          {perfil?.perfil === 'USER' && <NavLink to="/minhas-publicacoes" className={itemMenu}>
            <Files className="size-4 shrink-0" aria-hidden="true" /> Minhas publicações
          </NavLink>}
          <NavLink to="/favoritos" className={itemMenu}>
            <Star className="size-4" /> Favoritos
          </NavLink>
          <NavLink to="/configuracoes" className={itemMenu}>
            <Settings className="size-4" /> Configurações
          </NavLink>
          <button type="button" onClick={sairEVoltar} className="label-mono flex items-center gap-3 rounded-lg px-3 py-2 text-left text-muted-foreground transition-colors hover:bg-surface-container hover:text-primary">
            <LogOut className="size-4" /> Sair da conta
          </button>
          </nav>
          </div>
        </aside>

        <main className="min-w-0 flex-1" key={location.pathname}>
          {!['/painel', '/configuracoes', '/favoritos', '/forum'].includes(location.pathname.replace(/\/+$/, '')) && <BotaoVoltar destino="/painel" className="mb-4" />}
          {children}
        </main>
      </div>

      <footer className="shrink-0 border-t border-border bg-card">
        <div className="label-mono mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-6 text-muted-foreground">
          <span>© {new Date().getFullYear()} Banco Acadêmico · UNEMAT</span>
          <span className="flex items-center gap-1"><ShieldCheck className="size-3.5" /> Apoio institucional a estudantes de Computação</span>
        </div>
      </footer>
    </div>
  )
}
