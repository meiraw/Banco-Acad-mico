import { useNavigate } from 'react-router-dom'
import { AppShell } from '../components/AppShell.jsx'
import { FormularioDiscussao } from '../components/FormularioDiscussao.jsx'
import { api } from '../lib/api.js'

export function NovaDiscussao() {
  const navigate = useNavigate()
  async function criar(dados) {
    const discussao = await api.criarDiscussao(dados)
    navigate(`/forum/${discussao.id}`, { replace: true })
  }
  return <AppShell><div className="mx-auto max-w-3xl space-y-6">
    <header><h1 className="text-3xl font-bold">Nova discussão</h1><p className="mt-2 text-muted-foreground">Compartilhe sua dúvida e escolha a disciplina relacionada.</p></header>
    <FormularioDiscussao onSalvar={criar} onCancelar={() => navigate('/forum')} />
  </div></AppShell>
}
