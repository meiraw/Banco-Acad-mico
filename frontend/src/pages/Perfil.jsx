import { useEffect, useRef, useState } from 'react'
import { Camera, Upload } from 'lucide-react'
import { AppShell } from '../components/AppShell.jsx'
import { api, rotuloSemestre } from '../lib/api.js'
import { useSessao } from '../lib/sessao.jsx'
import { Avatar } from '../components/Avatar.jsx'
import { Administracao } from './Admin.jsx'

export function Perfil() {
  const { perfil, fotoUrl, erroPerfil, recarregarPerfil } = useSessao()
  const [arquivoFoto, setArquivoFoto] = useState(null)
  const [previaFoto, setPreviaFoto] = useState(null)
  const [salvandoFoto, setSalvandoFoto] = useState(false)
  const [erroFoto, setErroFoto] = useState('')
  const [mensagemFoto, setMensagemFoto] = useState('')
  const fotoInput = useRef(null)
  const envioFoto = useRef(false)
  const [cursos, setCursos] = useState([])
  const [semestres, setSemestres] = useState([])

  useEffect(() => { Promise.all([api.cursos(), api.semestres()]).then(([c, s]) => { setCursos(c); setSemestres(s) }).catch(() => {}) }, [])
  useEffect(() => () => { if (previaFoto) URL.revokeObjectURL(previaFoto) }, [previaFoto])

  function selecionarFoto(event) {
    const arquivo = event.target.files?.[0]
    event.target.value = ''
    if (!arquivo) return
    setErroFoto(''); setMensagemFoto('')
    if (!['image/jpeg', 'image/png'].includes(arquivo.type)) { setErroFoto('Escolha uma imagem JPG ou PNG.'); return }
    if (arquivo.size > 2 * 1024 * 1024) { setErroFoto('A foto deve ter no máximo 2 MB.'); return }
    setArquivoFoto(arquivo)
    setPreviaFoto(URL.createObjectURL(arquivo))
  }

  async function salvarFoto() {
    if (!arquivoFoto || envioFoto.current) return
    envioFoto.current = true
    setSalvandoFoto(true); setErroFoto(''); setMensagemFoto('')
    let salva = false
    try {
      await api.salvarFotoPerfil(arquivoFoto)
      salva = true
      await recarregarPerfil()
      setArquivoFoto(null); setPreviaFoto(null)
      setMensagemFoto('Foto de perfil atualizada.')
    } catch (error) {
      setErroFoto(salva ? 'A foto foi salva, mas não foi possível atualizar a visualização. Atualize a página.' : error.message)
    } finally { setSalvandoFoto(false); envioFoto.current = false }
  }

  return (
    <AppShell cursos={cursos} semestres={semestres} rotuloSemestre={rotuloSemestre}>
      <h1 className="text-3xl font-bold tracking-tight">Seu perfil</h1>

      <div className="card-surface mt-6 flex flex-col items-start gap-5 p-6 sm:flex-row sm:items-center">
        <Avatar nome={perfil?.nome} fotoUrl={previaFoto || fotoUrl} className="size-24 text-3xl ring-4 ring-background" />
        <div className="min-w-0 flex-1 space-y-3">
          <div>
          <p className="text-lg font-semibold">{perfil?.nome ?? 'Acadêmico'}</p>
          <p className="label-mono break-all text-muted-foreground">{perfil?.email}</p>
          </div>
          <input ref={fotoInput} type="file" accept="image/jpeg,image/png" onChange={selecionarFoto} disabled={salvandoFoto} className="sr-only" aria-label="Selecionar foto de perfil" />
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={() => fotoInput.current?.click()} disabled={salvandoFoto} className="btn btn-outline"><Camera className="size-4" />{fotoUrl || perfil?.possuiFoto ? 'Trocar foto' : 'Adicionar foto'}</button>
            {arquivoFoto && <>
              <button type="button" onClick={salvarFoto} disabled={salvandoFoto} className="btn btn-primary"><Upload className="size-4" />{salvandoFoto ? 'Salvando…' : 'Salvar foto'}</button>
              <button type="button" disabled={salvandoFoto} onClick={() => { setArquivoFoto(null); setPreviaFoto(null); setErroFoto('') }} className="btn btn-ghost">Cancelar</button>
            </>}
          </div>
          <p className="text-xs text-muted-foreground">JPG ou PNG, até 2 MB. A imagem será exibida em formato circular.</p>
          {(erroFoto || erroPerfil) && <p role="alert" className="text-sm text-destructive">{erroFoto || erroPerfil}</p>}
          {mensagemFoto && <p role="status" className="text-sm text-secondary">{mensagemFoto}</p>}
        </div>
      </div>

      {perfil?.perfil === 'ADMIN' && (
        <Administracao onGradeAtualizada={(novosCursos, novosSemestres) => {
          setCursos(novosCursos)
          setSemestres(novosSemestres)
        }} />
      )}

    </AppShell>
  )
}
