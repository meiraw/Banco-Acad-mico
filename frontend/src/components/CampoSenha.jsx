import { useState } from 'react'
import { Eye, EyeOff, Lock } from 'lucide-react'
import { avaliarSenha } from '../lib/academico.js'

const CORES = ['bg-border', 'bg-destructive', 'bg-tertiary', 'bg-secondary']

export function CampoSenha({ id, rotulo = 'Senha', valor, onChange, placeholder = '••••••••', autoComplete, comMedidor = false }) {
  const [visivel, setVisivel] = useState(false)
  const forca = avaliarSenha(valor)

  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="label-mono block">{rotulo}</label>
      <div className="relative">
        <Lock className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          id={id}
          type={visivel ? 'text' : 'password'}
          value={valor}
          onChange={e => onChange(e.target.value)}
          placeholder={placeholder}
          autoComplete={autoComplete}
          required
          className="input-base pl-9 pr-10"
        />
        <button
          type="button"
          onClick={() => setVisivel(v => !v)}
          aria-label={visivel ? 'Ocultar senha' : 'Mostrar senha'}
          title={visivel ? 'Ocultar senha' : 'Mostrar senha'}
          className="absolute right-2 top-1/2 grid size-7 -translate-y-1/2 place-items-center rounded-md text-muted-foreground transition-colors hover:bg-surface-container hover:text-foreground"
        >
          {visivel ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
        </button>
      </div>

      {comMedidor && (
        <div className="pt-1">
          <div className="flex gap-1.5">
            {[1, 2, 3].map(n => (
              <span key={n} className={`h-1.5 flex-1 rounded-full transition-colors ${forca.nivel >= n ? CORES[forca.nivel] : 'bg-border'}`} />
            ))}
          </div>
          <p className="label-mono mt-1.5 text-muted-foreground">
            {forca.nivel === 0
              ? 'Use números e caracteres especiais para uma senha forte.'
              : `Força: ${forca.rotulo}${forca.dicas.length ? ` · falta ${forca.dicas.join(', ')}` : ' · ótima escolha!'}`}
          </p>
        </div>
      )}
    </div>
  )
}
