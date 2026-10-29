import { useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Mail, User, ArrowRight, ArrowLeft, MailCheck } from 'lucide-react'
import { Logo } from '../components/Logo.jsx'
import { CampoSenha } from '../components/CampoSenha.jsx'
import { isEmailValido } from '../lib/academico.js'
import { CURSO_ACADEMICO, validarCadastro } from '../lib/cadastro.js'
import { useSessao } from '../lib/sessao.jsx'
import { api } from '../lib/api.js'

export function Auth() {
  const navigate = useNavigate()
  const location = useLocation()
  const { entrar } = useSessao()
  // A tela de redefinicao pode mandar o usuario direto para "Recuperar acesso" ao pedir um novo link.
  const [tela, setAba] = useState(location.state?.aba === 'recuperar' ? 'recuperar' : 'login')
  const aba = location.pathname === '/cadastro' ? 'cadastro' : tela
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState(location.state?.email || '')
  const [senha, setSenha] = useState('')
  const [confirmacaoSenha, setConfirmacaoSenha] = useState('')
  const [semestreAtual, setSemestreAtual] = useState('')
  const envioEmCurso = useRef(false)
  const [erro, setErro] = useState(location.state?.mensagem || '')
  const [enviando, setEnviando] = useState(false)
  const [emailEnviado, setEmailEnviado] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    if (envioEmCurso.current) return
    setErro('')
    if (!isEmailValido(email)) {
      setErro('Informe um e-mail válido, como nome@gmail.com.')
      return
    }
    if (aba === 'recuperar') {
      try {
        envioEmCurso.current = true
        setEnviando(true)
        await api.esqueciSenha(email.trim().toLowerCase())
        setEmailEnviado(true) // A resposta e sempre generica: nao revela se a conta existe.
      } catch (error) { setErro(error.status === 429 ? 'Muitas tentativas. Aguarde até 15 minutos e tente de novo.' : error.message) }
      finally { setEnviando(false); envioEmCurso.current = false }
      return
    }
    if (aba === 'cadastro') {
      const mensagem = validarCadastro({ nome, email, semestreAtual, senha, confirmacaoSenha })
      if (mensagem) { setErro(mensagem); return }
    } else if (senha.length < 6) { setErro('A senha precisa ter pelo menos 6 caracteres.'); return }
    let cadastroRealizado = false
    try {
      envioEmCurso.current = true
      setEnviando(true)
      const dados = { email: email.trim().toLowerCase(), senha }
      if (aba === 'cadastro') {
        await api.cadastrar({ ...dados, nome: nome.trim(), semestreAtual: Number(semestreAtual) })
        cadastroRealizado = true
      }
      const { token } = await api.login(dados)
      entrar({ nome: nome.trim() || dados.email.split('@')[0], email: dados.email, token })
      navigate('/painel', { replace: true })
    } catch (error) {
      if (cadastroRealizado) {
        setSenha('')
        setConfirmacaoSenha('')
        navigate('/auth', { replace: true, state: {
          email: email.trim().toLowerCase(),
          mensagem: 'Sua conta foi criada, mas não foi possível entrar automaticamente. Entre com seu e-mail e senha.',
        } })
      } else setErro(error.status >= 500 ? 'O servidor não conseguiu concluir a solicitação. Tente novamente em instantes.' : error.message)
    }
    finally { setEnviando(false); envioEmCurso.current = false }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-6 text-center">
          <div>
            <Link to="/" className="inline-block max-w-full">
              <Logo className="w-72 sm:w-80" />
            </Link>
            <p className="label-mono mt-2 text-muted-foreground">Apoio institucional para acadêmicos da UNEMAT</p>
          </div>
        </div>

        {aba === 'recuperar' && emailEnviado ? (
          <div className="card-surface space-y-4 p-6">
            <MailCheck className="size-8 text-secondary" />
            <div>
              <h1 className="text-xl font-semibold">Verifique seu e-mail</h1>
              <p className="mt-1 text-sm text-muted-foreground">
                Se houver uma conta para <span className="font-medium text-foreground">{email.trim().toLowerCase()}</span>, enviamos um link de redefinição. O link vale por 15 minutos e só pode ser usado uma vez.
              </p>
            </div>
            <button className="btn btn-outline w-full" onClick={() => { setEmailEnviado(false); setAba('login'); setErro('') }}>
              <ArrowLeft className="size-4" /> Voltar para o login
            </button>
          </div>
        ) : (
          <form onSubmit={enviar} className="card-surface space-y-4 p-6">
            <fieldset disabled={enviando} className="min-w-0 space-y-4">
            <div>
              <h1 className="text-xl font-semibold">
                {aba === 'login' ? 'Bem-vindo de volta' : aba === 'cadastro' ? 'Crie sua conta acadêmica' : 'Recuperar acesso'}
              </h1>
              <p className="mt-1 text-sm text-muted-foreground">
                {aba === 'recuperar' ? 'Informe o e-mail cadastrado para recuperar sua senha.' : 'Use seu e-mail e uma senha exclusiva do Banco Acadêmico.'}
              </p>
            </div>

            {aba === 'cadastro' && (
              <div className="space-y-1.5">
                <label htmlFor="nome" className="label-mono block">Nome completo</label>
                <div className="relative">
                  <User className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                  <input id="nome" value={nome} onChange={e => setNome(e.target.value)} placeholder="Maria Silva" maxLength={200} autoComplete="name" required className="input-base pl-9" />
                </div>
              </div>
            )}

            <div className="space-y-1.5">
              <label htmlFor="email" className="label-mono block">E-mail</label>
              <div className="relative">
                <Mail className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <input id="email" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="nome@gmail.com" maxLength={aba === 'cadastro' ? 200 : 255} autoComplete="email" required className="input-base pl-9" />
              </div>
            </div>

            {aba === 'cadastro' && <>
              <div className="space-y-1.5">
                <label htmlFor="cadastro-curso" className="label-mono block">Curso</label>
                <input id="cadastro-curso" value={CURSO_ACADEMICO} readOnly className="input-base bg-muted text-muted-foreground" />
              </div>
              <div className="space-y-1.5">
                <label htmlFor="cadastro-semestre" className="label-mono block">Semestre atual</label>
                <select id="cadastro-semestre" value={semestreAtual} onChange={e => setSemestreAtual(e.target.value)} required className="input-base">
                  <option value="">Selecione seu semestre</option>
                  {Array.from({ length: 8 }, (_, i) => i + 1).map(numero => <option key={numero} value={numero}>{numero}º semestre</option>)}
                </select>
              </div>
            </>}

            {aba !== 'recuperar' && (
              <CampoSenha id="senha" valor={senha} onChange={setSenha} autoComplete={aba === 'login' ? 'current-password' : 'new-password'} comMedidor={aba === 'cadastro'} />
            )}

            {aba === 'cadastro' && <CampoSenha id="confirmacao-senha" rotulo="Confirmar senha" valor={confirmacaoSenha} onChange={setConfirmacaoSenha} autoComplete="new-password" />}

            {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}

            <button type="submit" disabled={enviando} className="btn btn-primary w-full">
              {enviando ? (aba === 'cadastro' ? 'Criando sua conta…' : 'Enviando…') : aba === 'login' ? 'Entrar no portal' : aba === 'cadastro' ? 'Criar minha conta' : 'Enviar link de redefinição'}
              <ArrowRight className="size-4" />
            </button>

            {aba === 'cadastro' && <p className="text-center text-sm text-muted-foreground">
              Já possui uma conta? <Link to="/auth" className="font-semibold text-primary hover:underline">Entrar</Link>
            </p>}

            {aba === 'login' && <>
              <Link to="/cadastro" aria-disabled={enviando} onClick={e => { if (enviando) e.preventDefault() }} className="btn btn-outline w-full">
                Criar conta
              </Link>
              <button type="button" onClick={() => { setAba('recuperar'); setErro('') }} className="label-mono block w-full text-center text-muted-foreground transition-colors hover:text-primary">
                Esqueci minha senha
              </button>
            </>}
            {aba === 'recuperar' && (
              <button type="button" onClick={() => { setAba('login'); setErro('') }} className="label-mono flex w-full items-center justify-center gap-1 text-muted-foreground transition-colors hover:text-primary">
                <ArrowLeft className="size-3.5" /> Voltar para o login
              </button>
            )}
            </fieldset>
          </form>
        )}
      </div>
    </div>
  )
}
