import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { GraduationCap, Mail, User, ArrowRight, ArrowLeft, MailCheck } from 'lucide-react'
import { CampoSenha } from '../components/CampoSenha.jsx'
import { ThemeToggle } from '../components/ThemeToggle.jsx'
import { isEmailInstitucional, avaliarSenha } from '../lib/academico.js'
import { useSessao } from '../lib/sessao.jsx'

export function Auth() {
  const navigate = useNavigate()
  const { entrar } = useSessao()
  const [aba, setAba] = useState('login')
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [emailEnviado, setEmailEnviado] = useState(false)

  function enviar(e) {
    e.preventDefault()
    setErro('')
    if (!isEmailInstitucional(email)) {
      setErro('Use seu e-mail institucional da UNEMAT (ex.: nome.sobrenome@unemat.br).')
      return
    }
    if (aba === 'recuperar') { setEmailEnviado(true); return }
    if (senha.length < 6) { setErro('A senha precisa ter pelo menos 6 caracteres.'); return }
    if (aba === 'cadastro') {
      if (nome.trim().length < 3) { setErro('Informe seu nome completo.'); return }
      if (avaliarSenha(senha).nivel < 2) { setErro('Fortaleça sua senha: inclua números e caracteres especiais.'); return }
    }
    entrar({ nome: aba === 'cadastro' ? nome.trim() : nome.trim() || email.split('@')[0], email: email.trim().toLowerCase() })
    navigate('/painel', { replace: true })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-6 flex items-start justify-between gap-4">
          <div>
            <Link to="/" className="inline-flex items-center gap-2 text-primary">
              <GraduationCap className="size-7" />
              <span className="text-2xl font-bold tracking-tight">Banco Acadêmico</span>
            </Link>
            <p className="label-mono mt-2 text-muted-foreground">Apoio institucional para acadêmicos da UNEMAT</p>
          </div>
          <ThemeToggle />
        </div>

        {aba !== 'recuperar' && (
          <div className="mb-6 grid grid-cols-2 border-b border-border">
            {['login', 'cadastro'].map(t => (
              <button
                key={t}
                onClick={() => { setAba(t); setErro('') }}
                className={`label-mono -mb-px border-b-2 pb-3 font-medium transition-colors ${
                  aba === t ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'
                }`}
              >
                {t === 'login' ? 'Entrar' : 'Criar conta'}
              </button>
            ))}
          </div>
        )}

        {aba === 'recuperar' && emailEnviado ? (
          <div className="card-surface space-y-4 p-6">
            <MailCheck className="size-8 text-secondary" />
            <div>
              <h1 className="text-xl font-semibold">Verifique seu e-mail</h1>
              <p className="mt-1 text-sm text-muted-foreground">
                Se houver uma conta para <span className="font-medium text-foreground">{email.trim().toLowerCase()}</span>, enviamos um link de redefinição.
              </p>
            </div>
            <button className="btn btn-outline w-full" onClick={() => { setEmailEnviado(false); setAba('login') }}>
              <ArrowLeft className="size-4" /> Voltar para o login
            </button>
          </div>
        ) : (
          <form onSubmit={enviar} className="card-surface space-y-4 p-6">
            <div>
              <h1 className="text-xl font-semibold">
                {aba === 'login' ? 'Bem-vindo de volta' : aba === 'cadastro' ? 'Crie sua conta acadêmica' : 'Recuperar acesso'}
              </h1>
              <p className="mt-1 text-sm text-muted-foreground">
                {aba === 'recuperar' ? 'Informe seu e-mail institucional e enviaremos um link para criar uma nova senha.' : 'Somente e-mails institucionais da UNEMAT têm acesso à plataforma.'}
              </p>
            </div>

            {aba === 'cadastro' && (
              <div className="space-y-1.5">
                <label htmlFor="nome" className="label-mono block">Nome completo</label>
                <div className="relative">
                  <User className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                  <input id="nome" value={nome} onChange={e => setNome(e.target.value)} placeholder="Maria Silva" maxLength={100} required className="input-base pl-9" />
                </div>
              </div>
            )}

            <div className="space-y-1.5">
              <label htmlFor="email" className="label-mono block">E-mail institucional</label>
              <div className="relative">
                <Mail className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <input id="email" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="nome.sobrenome@unemat.br" maxLength={255} autoComplete="email" required className="input-base pl-9" />
              </div>
            </div>

            {aba !== 'recuperar' && (
              <CampoSenha id="senha" valor={senha} onChange={setSenha} autoComplete={aba === 'login' ? 'current-password' : 'new-password'} comMedidor={aba === 'cadastro'} />
            )}

            {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}

            <button type="submit" className="btn btn-primary w-full">
              {aba === 'login' ? 'Entrar no portal' : aba === 'cadastro' ? 'Criar conta' : 'Enviar link de redefinição'}
              <ArrowRight className="size-4" />
            </button>

            {aba === 'login' && (
              <button type="button" onClick={() => setAba('recuperar')} className="label-mono block w-full text-center text-muted-foreground transition-colors hover:text-primary">
                Esqueci minha senha
              </button>
            )}
            {aba === 'recuperar' && (
              <button type="button" onClick={() => setAba('login')} className="label-mono flex w-full items-center justify-center gap-1 text-muted-foreground transition-colors hover:text-primary">
                <ArrowLeft className="size-3.5" /> Voltar para o login
              </button>
            )}
          </form>
        )}
      </div>
    </div>
  )
}
