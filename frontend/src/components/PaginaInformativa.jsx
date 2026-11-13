import { Link } from 'react-router-dom'
import { AppShell } from './AppShell.jsx'
import { Logo } from './Logo.jsx'
import { ThemeToggle } from './ThemeToggle.jsx'
import { BotaoVoltar } from './BotaoVoltar.jsx'
import { useSessao } from '../lib/sessao.jsx'

export function PaginaInformativa({ titulo, descricao, children }) {
  const { perfil } = useSessao()
  const conteudo = <div className="mx-auto max-w-3xl space-y-6">
    <header><h1 className="text-3xl font-bold tracking-tight">{titulo}</h1><p className="mt-2 text-muted-foreground">{descricao}</p></header>
    {children}
    {perfil && <Link to="/configuracoes" className="btn btn-outline">Configurações</Link>}
  </div>
  if (perfil) return <AppShell>{conteudo}</AppShell>
  return <div className="flex min-h-screen flex-col bg-background">
    <header className="border-b border-border bg-card"><div className="mx-auto flex h-16 max-w-3xl items-center gap-4 px-4"><Link to="/" className="mr-auto min-w-0"><Logo className="w-36 sm:w-44" /></Link><ThemeToggle /></div></header>
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8 sm:py-12"><BotaoVoltar className="mb-4" />{conteudo}</main>
  </div>
}
