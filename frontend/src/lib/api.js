const BASE = import.meta.env.VITE_API_URL || '/api'

async function req(path, { publico, resposta, ...opts } = {}) {
  // Rotas publicas (ex.: recuperacao de senha) nao enviam Authorization: o backend rejeita
  // qualquer JWT invalido ou antigo presente no cabecalho, mesmo nessas rotas.
  const token = publico ? null : sessionStorage.getItem('banco-academico:token')
  opts = { ...opts, headers: { ...opts?.headers, ...(token ? { Authorization: `Bearer ${token}` } : {}) } }
  let res
  try { res = await fetch(BASE + path, opts) }
  catch { throw new Error('Não foi possível conectar ao servidor. Verifique se o backend está rodando.') }
  if (!res.ok) {
    let msg = `Erro ${res.status}`
    try { msg = (await res.json()).mensagem || msg } catch { /* corpo vazio */ }
    throw Object.assign(new Error(msg), { status: res.status })
  }
  return res.status === 204 ? null : resposta === 'blob' ? res.blob() : res.json()
}

const json = body => ({ headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
const lista = p => req(`${p}?size=500`).then(page => page.content)

// VIA_DTO mantém content e agrupa os metadados em page. Preserva o contrato das telas.
const reqPagina = path => req(path).then(resultado => {
  if (!resultado.page) return resultado // Compatibilidade enquanto o backend é reiniciado.
  const { content, page } = resultado
  return { content, ...page, first: page.number === 0, last: page.number + 1 >= page.totalPages }
})

export const api = {
  discussoes: (filtros = {}) => reqPagina(`/forum/discussoes?${new URLSearchParams(filtros)}`),
  discussao: id => req(`/forum/discussoes/${encodeURIComponent(id)}`),
  criarDiscussao: dados => req('/forum/discussoes', { method: 'POST', ...json(dados) }),
  editarDiscussao: (id, dados) => req(`/forum/discussoes/${encodeURIComponent(id)}`, { method: 'PUT', ...json(dados) }),
  excluirDiscussao: id => req(`/forum/discussoes/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  respostasForum: (id, pagina = 0) => reqPagina(`/forum/discussoes/${encodeURIComponent(id)}/respostas?page=${pagina}&size=20`),
  responderForum: (id, conteudo) => req(`/forum/discussoes/${encodeURIComponent(id)}/respostas`, { method: 'POST', ...json({ conteudo }) }),
  editarRespostaForum: (id, respostaId, dados) => req(`/forum/discussoes/${encodeURIComponent(id)}/respostas/${encodeURIComponent(respostaId)}`, { method: 'PUT', ...json(dados) }),
  excluirRespostaForum: (id, respostaId) => req(`/forum/discussoes/${encodeURIComponent(id)}/respostas/${encodeURIComponent(respostaId)}`, { method: 'DELETE' }),
  favoritos: (pagina = 0, tamanho = 12) => reqPagina(`/favoritos?page=${pagina}&size=${tamanho}`),
  favoritosIds: (pagina = 0) => reqPagina(`/favoritos/ids?page=${pagina}&size=200`),
  adicionarFavorito: id => req(`/favoritos/${encodeURIComponent(id)}`, { method: 'PUT' }),
  removerFavorito: id => req(`/favoritos/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  materiaisPendentes: (pagina = 0) => reqPagina(`/admin/materiais/pendentes?pagina=${pagina}`),
  aprovarMaterial: id => req(`/admin/materiais/${id}/aprovar`, { method: 'PATCH' }),
  rejeitarMaterial: (id, justificativa) => req(`/admin/materiais/${id}/rejeitar`, { method: 'PATCH', ...json({ justificativa }) }),
  arquivoMaterial: id => req(`/material/${id}/arquivo`, { resposta: 'blob' }),
  material: id => req(`/material/${id}`),
  comentarios: (id, pagina = 0) => req(`/material/${id}/comentarios?pagina=${pagina}`),
  comentar: (id, texto) => req(`/material/${id}/comentarios`, { method: 'POST', ...json({ texto }) }),
  curtida: id => req(`/material/${id}/curtida`),
  curtir: id => req(`/material/${id}/curtida`, { method: 'PUT' }),
  descurtir: id => req(`/material/${id}/curtida`, { method: 'DELETE' }),
  registrarDeslike: id => req(`/material/${id}/deslike`, { method: 'PUT' }),
  removerDeslike: id => req(`/material/${id}/deslike`, { method: 'DELETE' }),
  respostas: (id, comentarioId, pagina = 0) => req(`/material/${id}/comentarios/${comentarioId}/respostas?pagina=${pagina}`),
  responder: (id, comentarioId, texto, respostaPaiId) => req(`/material/${id}/comentarios/${comentarioId}/respostas`, { method: 'POST', ...json({ texto, ...(respostaPaiId ? { respostaPaiId } : {}) }) }),
  meuPerfil: () => req('/perfil'),
  atualizarPerfil: ({ semestreAtual }) => req('/perfil', { method: 'PUT', ...json({ semestreAtual }) }),
  alterarSenha: dados => req('/perfil/senha', { method: 'PUT', ...json(dados) }),
  minhasPublicacoes: (pagina = 0) => reqPagina(`/perfil/publicacoes?pagina=${pagina}`),
  fotoPerfil: () => req('/perfil/foto', { resposta: 'blob' }),
  salvarFotoPerfil: foto => {
    const dados = new FormData()
    dados.append('foto', foto)
    return req('/perfil/foto', { method: 'PUT', body: dados })
  },
  cadastrar: d => req('/usuario', { method: 'POST', publico: true, ...json(d) }),
  login: d => req('/auth/login', { method: 'POST', publico: true, ...json(d) }),
  confirmarEmail: token => req('/auth/confirmar-email', { method: 'POST', publico: true, ...json({ token }) }),
  reenviarConfirmacao: email => req('/auth/reenviar-confirmacao', { method: 'POST', publico: true, ...json({ email }) }),
  esqueciSenha: email => req('/auth/esqueci-senha', { method: 'POST', publico: true, ...json({ email }) }),
  validarCodigoSenha: (email, codigo) => req('/auth/validar-codigo-senha', { method: 'POST', publico: true, ...json({ email, codigo }) }),
  redefinirSenha: (token, novaSenha) => req('/auth/redefinir-senha', { method: 'POST', publico: true, ...json({ token, novaSenha }) }),
  cursos: () => req('/curso'),
  semestres: () => req('/semestres').then(s => s.sort((a, b) => a.numero - b.numero || a.nome.localeCompare(b.nome))),
  criarCurso: d => req('/curso', { method: 'POST', ...json(d) }),
  atualizarCurso: (id, d) => req(`/curso/${id}`, { method: 'PUT', ...json(d) }),
  excluirCurso: id => req(`/curso/${id}`, { method: 'DELETE' }),
  atualizarSemestre: (id, d) => req(`/semestres/${id}`, { method: 'PUT', ...json(d) }),
  excluirSemestre: id => req(`/semestres/${id}`, { method: 'DELETE' }),
  atualizarDisciplina: (id, d) => req(`/disciplinas/${id}`, { method: 'PUT', ...json(d) }),
  excluirDisciplina: id => req(`/disciplinas/${id}`, { method: 'DELETE' }),
  disciplinas: () => lista('/disciplinas'),
  materiais: () => lista('/material'),
  criarSemestre: d => req('/semestres', { method: 'POST', ...json(d) }),
  criarDisciplina: d => req('/disciplinas', { method: 'POST', ...json(d) }),
  excluirMaterial: id => req(`/material/${id}`, { method: 'DELETE' }),
  // O controller espera duas partes: "dados" (JSON) e "arquivo".
  criarMaterial(dados, arquivo) {
    const fd = new FormData()
    fd.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }))
    fd.append('arquivo', arquivo)
    return req('/material', { method: 'POST', body: fd })
  },
  // Requer o endpoint GET /material/{id}/arquivo no backend (ver README).
  urlArquivo: id => `${BASE}/material/${id}/arquivo`,
}

export function categoria(tipo = '') {
  if (tipo.includes('pdf')) return 'PDF'
  if (tipo.includes('presentation') || tipo.includes('powerpoint')) return 'Slides'
  if (tipo.startsWith('image/')) return 'Imagem'
  return 'Outro'
}

export const rotuloSemestre = s => `${s.numero}º semestre — ${s.nome}`
export const tamanho = b => (b == null ? '' : b > 1048576 ? `${(b / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(b / 1024))} KB`)
