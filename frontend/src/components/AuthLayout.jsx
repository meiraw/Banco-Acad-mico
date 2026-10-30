import { Link } from 'react-router-dom'
import { Logo } from './Logo.jsx'
import booksLogin from '../assets/images/books-login.png'

export function AuthLayout({ children, dividido = true }) {
  return <div className={dividido ? 'min-h-screen bg-background md:grid md:grid-cols-[45%_55%]' : 'flex min-h-screen items-center justify-center bg-background px-4 py-12'}>
    {dividido && <aside aria-label="Ilustração de livros" className="sticky top-0 hidden h-screen min-w-0 items-center justify-center self-start overflow-hidden bg-[#cccccc] md:flex">
      <img src={booksLogin} alt="Pilha de livros acadêmicos com capas rígidas e páginas brancas."
        className="h-auto max-h-full w-full object-contain" />
    </aside>}
    <main className={dividido ? 'flex min-h-screen min-w-0 items-center justify-center px-4 py-8 sm:px-8 lg:px-12' : 'w-full max-w-md'}>
      <div className="w-full max-w-md">
        <div className="mb-6 text-center">
          <Link to="/" className="inline-block max-w-full">
            <Logo className="w-72 max-w-full sm:w-80" />
          </Link>
          <p className="label-mono mt-2 text-muted-foreground">Apoio institucional para acadêmicos da UNEMAT</p>
        </div>
        {children}
      </div>
    </main>
  </div>
}
