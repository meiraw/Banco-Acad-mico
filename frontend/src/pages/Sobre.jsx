import { Link } from 'react-router-dom'
import { GraduationCap } from 'lucide-react'
import { ThemeToggle } from '../components/ThemeToggle.jsx'

export function Sobre() {
  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex h-16 max-w-3xl items-center gap-4 px-4">
          <Link to="/" className="flex items-center gap-2 text-primary">
            <GraduationCap className="size-6" />
            <span className="text-lg font-bold tracking-tight">Banco Acadêmico</span>
          </Link>
          <div className="flex-1" />
          <ThemeToggle />
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-16">
        <h1 className="text-3xl font-bold tracking-tight">Sobre o projeto</h1>
        <p className="mt-4 text-muted-foreground">
          O Banco Acadêmico é uma plataforma colaborativa criada por acadêmicos do curso de Ciência da Computação
          da UNEMAT, campus de Cáceres. O objetivo é centralizar resumos, listas de exercícios, slides e anotações
          que hoje circulam de forma dispersa por WhatsApp, e-mail e nuvem, organizando tudo por semestre e disciplina.
        </p>
        <p className="mt-4 text-muted-foreground">
          O Banco Acadêmico é um repositório de apoio: não substitui os sistemas oficiais da universidade nem
          funciona como um Ambiente Virtual de Aprendizagem.
        </p>
      </main>
    </div>
  )
}
