import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { GraduationCap, ArrowRight, ArrowLeft, ShieldCheck, LinkIcon } from 'lucide-react'
import { CampoSenha } from '../components/CampoSenha.jsx'
import { ThemeToggle } from '../components/ThemeToggle.jsx'
import { avaliarSenha } from '../lib/academico.js'
import { useSessao } from '../lib/sessao.jsx'
import { lerAutorizacao, guardarAutorizacao } from '../lib/recuperacao.js'
import { api } from '../lib/api.js'

export function RedefinirSenha() {
  const navigate = useNavigate()
  const { sair } = useSessao()
  // O token fica apenas em memoria: nunca vai para localStorage, sessionStorage ou logs.
  const [token, setToken] = useState(lerAutorizacao)
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [concluido, setConcluido] = useState(false)
  const [linkExpirado, setLinkExpirado] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    if (enviando) return
    setErro('')
    if (novaSenha.trim().length === 0) { setErro('A senha não pode ser vazia.'); return }
    if (novaSenha.length < 8) { setErro('A senha precisa ter pelo menos 8 caracteres.'); return }
    if (new TextEncoder().encode(novaSenha).length > 72) { setErro('A senha pode ter no máximo 72 bytes (cerca de 72 caracteres sem acentos).'); return }
    if (avaliarSenha(novaSenha).nivel < 2) { setErro('Fortaleça sua senha: inclua números e caracteres especiais.'); return }
    if (novaSenha !== confirmacao) { setErro('A confirmação não é igual à nova senha.'); return }
    try {
      setEnviando(true)
      await api.redefinirSenha(token, novaSenha)
      // Descarta o token e qualquer sessao local; nao ha login automatico.
      guardarAutorizacao(null)
      setToken(null)
      setNovaSenha('')
      setConfirmacao('')
      sair()
      setConcluido(true)
    } catch (error) {
      if (error.status === 400) { guardarAutorizacao(null); setToken(null); setLinkExpirado(true) } // token invalido, expirado ou ja usado
      else if (error.status === 429) setErro('Muitas tentativas. Aguarde até 15 minutos e tente de novo.')
      else setErro(error.message)
    } finally { setEnviando(false) }
  }

  const linkInvalido = !concluido && !token

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

        {concluido ? (
          <div className="card-surface space-y-4 p-6">
            <ShieldCheck className="size-8 text-secondary" />
            <div>
              <h1 className="text-xl font-semibold">Senha alterada</h1>
              <p className="mt-1 text-sm text-muted-foreground">
                Entre com sua nova senha. Por segurança, as sessões abertas em outros dispositivos foram encerradas.
              </p>
            </div>
            <button className="btn btn-primary w-full" onClick={() => navigate('/auth', { replace: true })}>
              Ir para o login <ArrowRight className="size-4" />
            </button>
          </div>
        ) : linkInvalido ? (
          <div className="card-surface space-y-4 p-6">
            <LinkIcon className="size-8 text-destructive" />
            <div>
              <h1 className="text-xl font-semibold">{linkExpirado ? 'Autorização expirada' : 'Valide seu código primeiro'}</h1>
              <p className="mt-1 text-sm text-muted-foreground">
                {linkExpirado
                  ? 'Peça um novo código para continuar.'
                  : 'Valide o código recebido por e-mail para continuar.'}
              </p>
            </div>
            <button className="btn btn-primary w-full" onClick={() => navigate('/auth', { replace: true, state: { aba: 'recuperar' } })}>
              Pedir novo código <ArrowRight className="size-4" />
            </button>
            <Link to="/auth" className="label-mono flex w-full items-center justify-center gap-1 text-muted-foreground transition-colors hover:text-primary">
              <ArrowLeft className="size-3.5" /> Voltar para o login
            </Link>
          </div>
        ) : (
          <form onSubmit={enviar} className="card-surface space-y-4 p-6">
            <div>
              <h1 className="text-xl font-semibold">Crie uma nova senha</h1>
              <p className="mt-1 text-sm text-muted-foreground">Escolha uma senha com pelo menos 8 caracteres, números e caracteres especiais.</p>
            </div>

            <CampoSenha id="nova-senha" rotulo="Nova senha" valor={novaSenha} onChange={setNovaSenha} autoComplete="new-password" comMedidor />
            <CampoSenha id="confirmacao-senha" rotulo="Confirmar nova senha" valor={confirmacao} onChange={setConfirmacao} autoComplete="new-password" />

            {erro && <p className="text-sm text-destructive" role="alert">{erro}</p>}

            <button type="submit" disabled={enviando} className="btn btn-primary w-full">
              Salvar nova senha <ArrowRight className="size-4" />
            </button>
            <Link to="/auth" className="label-mono flex w-full items-center justify-center gap-1 text-muted-foreground transition-colors hover:text-primary">
              <ArrowLeft className="size-3.5" /> Voltar para o login
            </Link>
          </form>
        )}
      </div>
    </div>
  )
}
