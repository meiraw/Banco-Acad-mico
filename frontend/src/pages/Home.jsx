import { Link } from 'react-router-dom'
import { GraduationCap, ArrowRight, BookOpen, Users, Star } from 'lucide-react'
import { ThemeToggle } from '../components/ThemeToggle.jsx'

const DESTAQUES = [
  { Icone: BookOpen, titulo: 'Organizado por semestre', texto: 'Materiais reunidos exatamente pela grade do curso.' },
  { Icone: Users, titulo: 'Feito pelos colegas', texto: 'Cada resumo, lista e slide vem de quem já cursou a disciplina.' },
  { Icone: Star, titulo: 'Fácil de encontrar', texto: 'Busca por título ou disciplina, com filtro por tipo de arquivo.' },
]

export function Home() {
  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4">
          <Link to="/" className="flex items-center gap-2 text-primary">
            <GraduationCap className="size-6" />
            <span className="text-lg font-bold tracking-tight">Banco Acadêmico</span>
          </Link>
          <nav className="label-mono ml-auto hidden items-center gap-6 text-muted-foreground sm:flex">
            <Link to="/sobre" className="hover:text-primary">Sobre</Link>
            <Link to="/termos" className="hover:text-primary">Termos</Link>
          </nav>
          <ThemeToggle />
          <Link to="/auth" className="btn btn-primary">Entrar</Link>
        </div>
      </header>

      <main>
        <section className="mx-auto max-w-6xl px-4 py-20">
          <p className="label-mono text-secondary">Ciência da Computação · UNEMAT</p>
          <h1 className="mt-3 max-w-3xl text-4xl font-bold tracking-tight sm:text-6xl">
            O acervo de estudos feito pelos próprios acadêmicos
          </h1>
          <p className="mt-5 max-w-2xl text-lg text-muted-foreground">
            Resumos, listas resolvidas, slides e anotações organizados por semestre e disciplina.
            Um repositório de apoio — não substitui os sistemas oficiais da universidade.
          </p>
          <div className="mt-8">
            <Link to="/auth" className="btn btn-primary text-base px-6 py-3">
              Entrar com e-mail institucional <ArrowRight className="size-4" />
            </Link>
          </div>

          <div className="mt-16 grid gap-4 sm:grid-cols-3">
            {DESTAQUES.map(c => (
              <div key={c.titulo} className="card-surface p-6">
                <c.Icone className="size-6 text-primary" />
                <h2 className="mt-3 font-semibold">{c.titulo}</h2>
                <p className="mt-1 text-sm text-muted-foreground">{c.texto}</p>
              </div>
            ))}
          </div>
        </section>
      </main>

      <footer className="label-mono mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-4 py-8 text-muted-foreground">
        <span>© {new Date().getFullYear()} Banco Acadêmico Colaborativo · UNEMAT</span>
        <span className="flex gap-4">
          <Link to="/sobre" className="hover:text-primary">Sobre</Link>
          <Link to="/termos" className="hover:text-primary">Termos</Link>
        </span>
      </footer>
    </div>
  )
}
