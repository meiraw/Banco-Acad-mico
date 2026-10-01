const BASE = '/api'

async function req(path, opts) {
  let res
  try { res = await fetch(BASE + path, opts) }
  catch { throw new Error('Não foi possível conectar ao servidor. Verifique se o backend está rodando.') }
  if (!res.ok) {
    let msg = `Erro ${res.status}`
    try { msg = (await res.json()).mensagem || msg } catch { /* corpo vazio */ }
    throw new Error(msg)
  }
  return res.status === 204 ? null : res.json()
}

const json = body => ({ headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
const lista = p => req(`${p}?size=500`).then(page => page.content)

export const api = {
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
