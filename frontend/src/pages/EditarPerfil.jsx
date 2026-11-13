import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { AppShell } from '../components/AppShell.jsx'
import { FotoPerfil } from '../components/FotoPerfil.jsx'
import { useSessao } from '../lib/sessao.jsx'
import { api } from '../lib/api.js'

export function EditarPerfil() {
  const { perfil, recarregarPerfil } = useSessao()
  const [nome, setNome] = useState(perfil?.nome || '')
  const [semestre, setSemestre] = useState(perfil?.semestreAtual || '')
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const envio = useRef(false)
  const [tentativa, setTentativa] = useState(0)
  useEffect(() => {
    let ativo = true
    setCarregando(true); setErro('')
    api.meuPerfil().then(p => { if (ativo) { setNome(p.nome); setSemestre(p.semestreAtual || '') } })
      .catch(e => { if (ativo) setErro(e.message) }).finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [tentativa])
  async function salvar(e) {
    e.preventDefault()
    if (envio.current) return
    if (!semestre) { setErro('Selecione o semestre.'); return }
    envio.current = true; setSalvando(true); setErro(''); setMensagem('')
    let salvo = false
    try {
      const atualizado = await api.atualizarPerfil({ semestreAtual: Number(semestre) })
      salvo = true; setNome(atualizado.nome); setSemestre(atualizado.semestreAtual)
      await recarregarPerfil()
      setMensagem('Perfil atualizado com sucesso.')
    } catch (e) { setErro(salvo ? 'Seus dados foram salvos, mas a visualização não foi atualizada. Recarregue a página.' : e.message) }
    finally { envio.current = false; setSalvando(false) }
  }
  return <AppShell><div className="mx-auto max-w-2xl space-y-6">
    <header><h1 className="text-3xl font-bold tracking-tight">Editar perfil</h1><p className="mt-1 text-muted-foreground">Consulte seus dados e atualize seu semestre.</p></header>
    <FotoPerfil />
    <form onSubmit={salvar} className="card-surface space-y-5 p-5 sm:p-6">
      {carregando && <p role="status" className="text-sm text-muted-foreground">Carregando seus dados...</p>}
      <dl className="space-y-4">
        <div><dt className="label-mono mb-1 text-muted-foreground">Nome completo</dt><dd className="break-words font-medium">{nome || '—'}</dd></div>
        <div><dt className="label-mono mb-1 text-muted-foreground">E-mail</dt><dd className="break-words">{perfil?.email || '—'}</dd></div>
        <div><dt className="label-mono mb-1 text-muted-foreground">Curso</dt><dd className="break-words">{perfil?.curso || 'Ciência da Computação'}</dd></div>
      </dl>
      <div><label htmlFor="perfil-semestre" className="label-mono mb-2 block">Semestre atual</label><select id="perfil-semestre" className="input-base" required value={semestre} onChange={e => setSemestre(e.target.value)} disabled={carregando || salvando}><option value="">Selecione seu semestre</option>{Array.from({ length: 8 }, (_, i) => <option key={i + 1} value={i + 1}>{i + 1}º semestre</option>)}</select></div>
      {erro && <p role="alert" className="text-sm text-destructive">{erro}</p>}
      {mensagem && <p role="status" className="text-sm text-secondary">{mensagem}</p>}
      <div className="flex flex-wrap justify-end gap-3"><Link to="/configuracoes" className="btn btn-outline">Configurações</Link><button disabled={carregando || salvando} className="btn btn-primary">{salvando ? 'Salvando...' : 'Salvar alterações'}</button></div>
      {erro && <button type="button" disabled={salvando || carregando} onClick={() => setTentativa(t => t + 1)} className="text-sm text-secondary underline">Recarregar dados</button>}
    </form>
  </div></AppShell>
}
