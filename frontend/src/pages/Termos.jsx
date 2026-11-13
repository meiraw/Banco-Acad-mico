import { Link } from 'react-router-dom'
import { Logo } from '../components/Logo.jsx'
import { ThemeToggle } from '../components/ThemeToggle.jsx'
import { BotaoVoltar } from '../components/BotaoVoltar.jsx'

export function Termos() {
  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex h-16 max-w-3xl items-center gap-4 px-4">
          <Link to="/" className="min-w-0">
            <Logo className="w-36 sm:w-44" />
          </Link>
          <div className="flex-1" />
          <ThemeToggle />
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-16 space-y-6 text-muted-foreground">
        <BotaoVoltar />
        <h1 className="text-3xl font-bold tracking-tight text-foreground">Termos de uso</h1>
        <p>Ao enviar um material, você declara ser responsável pelo conteúdo compartilhado e confirma que ele não viola direitos autorais de terceiros, conforme o Marco Civil da Internet (Lei nº 12.965/2014).</p>
        <p>Materiais protegidos por direitos autorais devem trazer a referência bibliográfica completa no momento do envio.</p>
        <p>Nunca envie provas, trabalhos ou anotações contendo dados pessoais sensíveis — CPF, telefone, matrícula ou notas — sem antes rasurar essas informações, conforme a Lei Geral de Proteção de Dados (Lei nº 13.709/2018).</p>
        <p>A equipe do Banco Acadêmico pode remover materiais que violem estes termos ou que sejam denunciados pela comunidade.</p>
      </main>
    </div>
  )
}
