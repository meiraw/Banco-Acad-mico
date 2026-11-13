export const TIPOS_PUBLICACAO = [
  ['SLIDE', 'Slide'], ['ANOTACAO', 'Anotação'], ['RESUMO', 'Resumo'],
  ['LISTA_EXERCICIOS', 'Lista de exercícios'], ['RESOLUCAO_EXERCICIOS', 'Resolução de exercícios'],
  ['LIVRO', 'Livro'], ['ARTIGO', 'Artigo'],
]

export const rotuloTipoPublicacao = tipo => TIPOS_PUBLICACAO.find(([valor]) => valor === tipo)?.[1] || 'Não informado'
export const permiteReferencia = tipo => tipo === 'LIVRO' || tipo === 'ARTIGO'

export function dadosPublicacao(tipoPublicacao, referencia) {
  if (!TIPOS_PUBLICACAO.some(([valor]) => valor === tipoPublicacao)) throw new Error('Selecione o tipo de publicação.')
  const referenciaNormalizada = (referencia ?? '').trim()
  if (tipoPublicacao === 'ARTIGO' && !referenciaNormalizada) throw new Error('Informe a referência bibliográfica do artigo.')
  return { tipoPublicacao, referenciaBibliografica: permiteReferencia(tipoPublicacao) ? referenciaNormalizada || null : null }
}
