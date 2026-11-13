import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('fórum usa Bearer, filtros paginados e contratos sem autor escolhido', async t => {
  const fetchOriginal = globalThis.fetch, storageOriginal = globalThis.sessionStorage
  t.after(() => { globalThis.fetch = fetchOriginal; globalThis.sessionStorage = storageOriginal })
  globalThis.sessionStorage = { getItem: () => 'token-teste' }
  const chamadas = []
  globalThis.fetch = async (url, options) => {
    chamadas.push({ url, options })
    return { ok: true, status: options.method === 'DELETE' ? 204 : 200,
      json: async () => ({ content: [], page: { number: 1, size: 20, totalPages: 3, totalElements: 43 } }) }
  }
  const resultado = await api.discussoes({ q: 'árvore & grafo', disciplinaId: 'disciplina', minhas: true, ordem: 'antigas', page: 1, size: 20 })
  assert.equal(resultado.number, 1); assert.equal(resultado.last, false); assert.equal(resultado.totalElements, 43)
  const filtros = new URL(chamadas[0].url, 'http://localhost').searchParams
  assert.equal(filtros.get('q'), 'árvore & grafo'); assert.equal(filtros.get('minhas'), 'true')
  assert.equal(filtros.get('disciplinaId'), 'disciplina'); assert.equal(filtros.get('page'), '1')
  await api.discussao('discussao')
  await api.criarDiscussao({ titulo: 'Título', disciplinaId: 'disciplina', conteudo: 'Dúvida' })
  await api.editarDiscussao('discussao', { titulo: 'Editado', disciplinaId: 'disciplina', conteudo: 'Conteúdo', versao: 2 })
  await api.respostasForum('discussao', 1)
  await api.responderForum('discussao', 'Explicação')
  await api.editarRespostaForum('discussao', 'resposta', { conteudo: 'Editada', versao: 3 })
  await api.excluirRespostaForum('discussao', 'resposta')
  await api.excluirDiscussao('discussao')
  assert.deepEqual(chamadas.slice(1).map(c => [c.url, c.options.method || 'GET']), [
    ['/api/forum/discussoes/discussao', 'GET'], ['/api/forum/discussoes', 'POST'],
    ['/api/forum/discussoes/discussao', 'PUT'], ['/api/forum/discussoes/discussao/respostas?page=1&size=20', 'GET'],
    ['/api/forum/discussoes/discussao/respostas', 'POST'], ['/api/forum/discussoes/discussao/respostas/resposta', 'PUT'],
    ['/api/forum/discussoes/discussao/respostas/resposta', 'DELETE'], ['/api/forum/discussoes/discussao', 'DELETE'],
  ])
  for (const { options } of chamadas) {
    assert.equal(options.headers.Authorization, 'Bearer token-teste')
    if (options.body) {
      assert.equal(options.headers['Content-Type'], 'application/json')
      assert.equal('autorId' in JSON.parse(options.body), false)
    }
  }
  assert.equal(JSON.parse(chamadas[3].options.body).versao, 2)
  assert.equal(JSON.parse(chamadas[6].options.body).versao, 3)
})

test('erros de conexão e conflito são propagados para a interface', async t => {
  const fetchOriginal = globalThis.fetch, storageOriginal = globalThis.sessionStorage
  t.after(() => { globalThis.fetch = fetchOriginal; globalThis.sessionStorage = storageOriginal })
  globalThis.sessionStorage = { getItem: () => 'token-teste' }
  globalThis.fetch = async () => { throw new TypeError('offline') }
  await assert.rejects(api.discussoes(), /Não foi possível conectar ao servidor/)
  globalThis.fetch = async () => ({ ok: false, status: 409, json: async () => ({ mensagem: 'O conteúdo mudou.' }) })
  await assert.rejects(api.editarRespostaForum('d', 'r', { conteudo: 'Novo', versao: 0 }), e => e.status === 409 && e.message === 'O conteúdo mudou.')
})
