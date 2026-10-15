import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../lib/api.js'

export function ConfirmarEmail() {
  const [token, setToken] = useState(() => {
    const valor = new URLSearchParams(window.location.hash.slice(1)).get('token')
    return /^[A-Za-z0-9_-]{43}$/.test(valor || '') ? valor : null
  })
  const [concluido, setConcluido] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  useEffect(() => {
    if (window.location.hash) window.history.replaceState(null, '', window.location.pathname + window.location.search)
    function novoLink() {
      const valor = new URLSearchParams(window.location.hash.slice(1)).get('token')
      setToken(/^[A-Za-z0-9_-]{43}$/.test(valor || '') ? valor : null)
      setConcluido(false); setErro('')
      window.history.replaceState(null, '', window.location.pathname + window.location.search)
    }
    window.addEventListener('hashchange', novoLink)
    return () => window.removeEventListener('hashchange', novoLink)
  }, [])
  async function confirmar() {
    if (enviando || !token) return
    setEnviando(true); setErro('')
    try {
      await api.confirmarEmail(token)
      setToken(null); setConcluido(true)
    } catch (error) {
      if (error.status === 400) setToken(null)
      setErro(error.message)
    } finally { setEnviando(false) }
  }
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <div className="card-surface w-full max-w-md space-y-4 p-6">
        <h1 className="text-xl font-semibold">{concluido ? 'E-mail confirmado' : 'Confirme seu e-mail'}</h1>
        <p className="text-sm text-muted-foreground">
          {concluido ? 'Sua conta está pronta. Entre com a senha que criou no Banco Acadêmico.'
            : token ? 'Clique abaixo para confirmar que este e-mail pertence a você. O link vale por 15 minutos.'
              : 'Link incompleto, inválido ou expirado. Volte ao login para solicitar outra confirmação.'}
        </p>
        {erro && <p role="alert" className="text-sm text-destructive">{erro}</p>}
        {token && !concluido && <button disabled={enviando} onClick={confirmar} className="btn btn-primary w-full">
          {enviando ? 'Confirmando...' : 'Confirmar meu e-mail'}
        </button>}
        <Link to="/auth" className="btn btn-outline w-full">Ir para o login</Link>
      </div>
    </main>
  )
}
