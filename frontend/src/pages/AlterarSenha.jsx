import { useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AppShell } from '../components/AppShell.jsx'
import { CampoSenha } from '../components/CampoSenha.jsx'
import { useSessao } from '../lib/sessao.jsx'
import { api } from '../lib/api.js'

export function AlterarSenha() {
  const { sair } = useSessao()
  const navigate = useNavigate()
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)
  const envio = useRef(false)
  async function salvar(e) {
    e.preventDefault()
    if (envio.current) return
    setErro('')
    if (!senhaAtual || !novaSenha.trim() || novaSenha.length < 8 || new TextEncoder().encode(novaSenha).length > 72) { setErro('Informe a senha atual e uma nova senha com pelo menos 8 caracteres e até 72 bytes UTF-8.'); return }
    if (novaSenha !== confirmacao) { setErro('A confirmação não corresponde à nova senha.'); return }
    envio.current = true; setSalvando(true)
    try {
      const resultado = await api.alterarSenha({ senhaAtual, novaSenha, confirmacaoNovaSenha: confirmacao })
      setSenhaAtual(''); setNovaSenha(''); setConfirmacao('')
      navigate('/auth', { replace: true, state: { senhaAlterada: resultado.mensagem } })
      sair()
    } catch (e) { setErro(e.message) }
    finally { envio.current = false; setSalvando(false) }
  }
  return <AppShell><div className="mx-auto max-w-2xl space-y-6">
    <header><h1 className="text-3xl font-bold tracking-tight">Alterar senha</h1><p className="mt-1 text-muted-foreground">Atualize sua senha para manter sua conta segura.</p></header>
    <form onSubmit={salvar} className="card-surface space-y-5 p-5 sm:p-6">
      <fieldset disabled={salvando} className="space-y-5">
        <CampoSenha id="senha-atual" rotulo="Senha atual" valor={senhaAtual} onChange={setSenhaAtual} autoComplete="current-password" />
        <CampoSenha id="nova-senha-conta" rotulo="Nova senha" valor={novaSenha} onChange={setNovaSenha} autoComplete="new-password" comMedidor />
        <CampoSenha id="confirmacao-senha-conta" rotulo="Confirmar nova senha" valor={confirmacao} onChange={setConfirmacao} autoComplete="new-password" />
      </fieldset>
      <p className="text-sm text-muted-foreground">Use pelo menos 8 caracteres e até 72 bytes UTF-8. Após salvar, você precisará entrar novamente. As outras sessões da conta também serão encerradas.</p>
      {erro && <p role="alert" className="text-sm text-destructive">{erro}</p>}
      <div className="flex flex-wrap justify-end gap-3"><Link to="/configuracoes" className="btn btn-outline">Cancelar</Link><button disabled={salvando} className="btn btn-primary">{salvando ? 'Salvando...' : 'Salvar nova senha'}</button></div>
    </form>
  </div></AppShell>
}
