import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, FileText, Info, Moon, Sun, Monitor, UserRound, ShieldCheck, CircleHelp } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { useTema } from '../lib/useTema.js'

function Opcao({ to, icone: Icone, titulo, descricao }) {
  return <Link to={to} className="flex items-center gap-3 rounded-lg px-2 py-4 transition-colors hover:bg-surface-container focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary">
    <Icone className="size-5 shrink-0 text-primary" aria-hidden="true" />
    <div className="min-w-0 flex-1"><p className="font-medium">{titulo}</p><p className="mt-1 text-sm text-muted-foreground">{descricao}</p></div>
    <ArrowRight className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
  </Link>
}
export function Configuracoes() {
  const { preferencia, selecionar, disponivel } = useTema()
  const [erroTema, setErroTema] = useState('')
  function escolher(valor) {
    setErroTema(selecionar(valor) ? '' : 'Não foi possível salvar o tema neste navegador. Verifique as permissões de armazenamento.')
  }
  return <AppShell><div className="mx-auto max-w-2xl space-y-6">
    <header><h1 className="text-3xl font-bold tracking-tight">Configurações</h1><p className="mt-1 text-muted-foreground">Gerencie sua conta, personalize a aparência e consulte as informações da plataforma.</p></header>
    <section aria-labelledby="conta-perfil" className="card-surface p-5 sm:p-6">
      <h2 id="conta-perfil" className="text-lg font-semibold">Conta e perfil</h2>
      <div className="mt-2 divide-y divide-border">
        <Opcao to="/configuracoes/perfil" icone={UserRound} titulo="Editar perfil" descricao="Consulte seus dados e atualize seu semestre." />
      </div>
    </section>
    <section aria-labelledby="aparencia" className="card-surface p-5 sm:p-6">
      <h2 id="aparencia" className="text-lg font-semibold">Aparência</h2>
      <p id="descricao-tema" className="mt-2 text-sm text-muted-foreground">Sua preferência fica salva para sua conta neste navegador. Automático acompanha o tema do dispositivo.</p>
      <fieldset disabled={!disponivel} aria-describedby="descricao-tema" className="mt-4 grid gap-3 sm:grid-cols-3">
        <legend className="sr-only">Escolha o tema</legend>
        {[['claro', 'Claro', Sun], ['escuro', 'Escuro', Moon], ['automatico', 'Automático', Monitor]].map(([valor, titulo, Icone]) => <label key={valor} className={`flex cursor-pointer items-center gap-3 rounded-lg border p-3 transition-colors ${preferencia === valor ? 'border-primary bg-surface-container text-primary' : 'border-border hover:bg-surface-container'}`}>
          <input type="radio" name="tema" value={valor} checked={preferencia === valor} onChange={() => escolher(valor)} className="accent-primary" />
          <Icone className="size-5 shrink-0" aria-hidden="true" /><span className="text-sm font-medium">{titulo}</span>
        </label>)}
      </fieldset>
      {erroTema && <p role="alert" className="mt-3 text-sm text-destructive">{erroTema}</p>}
    </section>
    <section aria-labelledby="informacoes" className="card-surface p-5 sm:p-6">
      <h2 id="informacoes" className="text-lg font-semibold">Informações e suporte</h2>
      <div className="mt-2 divide-y divide-border">
        <Opcao to="/sobre" icone={Info} titulo="Sobre o Banco Acadêmico" descricao="Conheça a proposta e os objetivos da plataforma." />
        <Opcao to="/termos" icone={FileText} titulo="Termos de uso" descricao="Consulte as regras de utilização e compartilhamento." />
        <Opcao to="/privacidade" icone={ShieldCheck} titulo="Privacidade" descricao="Saiba como seus dados pessoais são tratados." />
        <Opcao to="/ajuda" icone={CircleHelp} titulo="Ajuda" descricao="Encontre orientações para utilizar o Banco Acadêmico." />
      </div>
    </section>
  </div></AppShell>
}
