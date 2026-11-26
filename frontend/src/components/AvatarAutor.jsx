import { useEffect, useState } from 'react'
import { api } from '../lib/api.js'
import { useSessao } from '../lib/sessao.jsx'
import { Avatar } from './Avatar.jsx'

export function AvatarAutor({ materialId, comentarioId, discussaoId, respostaId, autorNome, className }) {
  const { perfil } = useSessao()
  const token = perfil?.token
  const [foto, setFoto] = useState(null)
  const chave = [materialId, comentarioId, discussaoId, respostaId].join('/')

  useEffect(() => {
    if (!token || (!materialId && !discussaoId)) return
    let ativo = true
    let url
    const consulta = discussaoId
      ? api.fotoAutorForum(discussaoId, respostaId)
      : api.fotoAutorMaterial(materialId, comentarioId, respostaId)
    consulta.then(blob => {
      if (!ativo) return
      url = URL.createObjectURL(blob)
      setFoto({ chave, token, url })
    }).catch(() => {
      // As iniciais permanecem quando a foto está indisponível.
    })
    return () => {
      ativo = false
      if (url) URL.revokeObjectURL(url)
    }
  }, [chave, materialId, comentarioId, discussaoId, respostaId, token])

  const fotoUrl = foto && foto.chave === chave && foto.token === token ? foto.url : null
  return <Avatar nome={autorNome} fotoUrl={fotoUrl} className={className} />
}
