import { useEffect, useRef, useState } from 'react'
import { ThumbsUp, ThumbsDown } from 'lucide-react'
import { api } from '../lib/api.js'

export function CurtirMaterial({ materialId }) {
  const [estado, setEstado] = useState({ total: 0, curtido: false, totalDeslikes: 0, deslike: false })
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [tentativa, setTentativa] = useState(0)
  const envio = useRef(false)

  useEffect(() => {
    let ativo = true
    setCarregando(true); setErro('')
    api.curtida(materialId).then(dados => { if (ativo) setEstado(dados) })
      .catch(e => { if (ativo) setErro(e.message) })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [materialId, tentativa])

  async function alternar(negativa = false) {
    if (envio.current || carregando) return
    envio.current = true; setCarregando(true); setErro('')
    try {
      const acao = negativa
        ? estado.deslike ? api.removerDeslike : api.registrarDeslike
        : estado.curtido ? api.descurtir : api.curtir
      setEstado(await acao(materialId))
    }
    catch (e) { setErro(e.message) }
    finally { envio.current = false; setCarregando(false) }
  }

  return <div className="space-y-2">
    <div role="group" aria-label="Reações à publicação" className="flex flex-wrap gap-2">
    <button type="button" onClick={() => alternar()} disabled={carregando || !!erro} aria-pressed={estado.curtido}
      title={estado.curtido ? 'Remover minha curtida' : 'Curtir publicação'}
      className={`btn btn-outline ${estado.curtido ? 'text-primary' : ''}`}>
      <ThumbsUp className={`size-4 ${estado.curtido ? 'fill-current' : ''}`} />
      {carregando ? 'Carregando…' : estado.curtido ? 'Curtido' : 'Curtir'}
      <span aria-label={`${estado.total} curtidas`}>({estado.total})</span>
    </button>
    <button type="button" onClick={() => alternar(true)} disabled={carregando || !!erro} aria-pressed={estado.deslike}
      title={estado.deslike ? 'Remover minha reação negativa' : 'Reagir negativamente à publicação'}
      className={`btn btn-outline ${estado.deslike ? 'text-destructive' : ''}`}>
      <ThumbsDown className={`size-4 ${estado.deslike ? 'fill-current' : ''}`} />
      {carregando ? 'Carregando…' : estado.deslike ? 'Descurtido' : 'Descurtir'}
      <span aria-label={`${estado.totalDeslikes ?? 0} reações negativas`}>({estado.totalDeslikes ?? 0})</span>
    </button>
    </div>
    {erro && <p role="alert" className="max-w-sm text-sm text-destructive">{erro} <button type="button" onClick={() => setTentativa(t => t + 1)} className="underline">Tentar novamente</button></p>}
  </div>
}
