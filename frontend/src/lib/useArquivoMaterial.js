import { useEffect, useState } from 'react'
import { api } from './api.js'

// Tags img/iframe não enviam o Bearer. Materiais restritos usam fetch autenticado.
export function useArquivoMaterial(material) {
  const [resultado, setResultado] = useState(null)
  const restrito = material?.statusModeracao && material.statusModeracao !== 'APROVADO'
  const id = material?.id
  useEffect(() => {
    if (!id || !restrito) return
    let ativo = true
    let url
    setResultado(null)
    api.arquivoMaterial(id).then(blob => {
      if (!ativo) return
      url = URL.createObjectURL(blob)
      setResultado({ id, url })
    }).catch(e => { if (ativo) setResultado({ id, erro: e.message }) })
    return () => { ativo = false; if (url) URL.revokeObjectURL(url) }
  }, [id, restrito])
  return restrito
    ? { url: resultado?.id === id ? resultado.url : null, erro: resultado?.id === id ? resultado.erro : null }
    : { url: id ? api.urlArquivo(id) : null, erro: null }
}
