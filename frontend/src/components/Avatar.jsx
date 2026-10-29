import { useState } from 'react'
import { iniciais } from '../lib/academico.js'

export function Avatar({ nome, fotoUrl, className = '' }) {
  const [fotoComErro, setFotoComErro] = useState(null)
  return <span className={`inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-primary font-semibold text-primary-foreground ${className}`}>
    {fotoUrl && fotoComErro !== fotoUrl
      ? <img src={fotoUrl} alt={`Foto de ${nome || 'perfil'}`} onError={() => setFotoComErro(fotoUrl)} className="h-full w-full object-cover" />
      : iniciais(nome || 'Acadêmico')}
  </span>
}
