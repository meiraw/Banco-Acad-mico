import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('favoritos usam Bearer, paginação e operações idempotentes sem ID de usuário', async t => {
  const fetchOriginal = globalThis.fetch, storageOriginal = globalThis.sessionStorage
  t.after(() => { globalThis.fetch = fetchOriginal; globalThis.sessionStorage = storageOriginal })
  globalThis.sessionStorage = { getItem: () => 'token-conta' }
  const chamadas = []
  globalThis.fetch = async (url, options) => {
    chamadas.push({ url, options })
    return { ok: true, status: options.method ? 204 : 200,
      json: async () => ({ content: [], page: { number: 1, totalPages: 2, totalElements: 13 } }) }
  }
  const pagina = await api.favoritos(1)
  await api.favoritosIds(1); await api.adicionarFavorito('material'); await api.removerFavorito('material')
  assert.deepEqual(chamadas.map(c => c.url), ['/api/favoritos?page=1&size=12', '/api/favoritos/ids?page=1&size=200', '/api/favoritos/material', '/api/favoritos/material'])
  assert.equal(pagina.last, true)
  assert.equal(chamadas[2].options.method, 'PUT'); assert.equal(chamadas[3].options.method, 'DELETE')
  for (const chamada of chamadas) {
    assert.equal(chamada.options.headers.Authorization, 'Bearer token-conta')
    assert.equal(chamada.options.body, undefined)
  }
  globalThis.fetch = async () => ({ ok: false, status: 404, json: async () => ({ mensagem: 'Material não encontrado.' }) })
  await assert.rejects(api.adicionarFavorito('removido'), e => e.status === 404 && e.message === 'Material não encontrado.')
})
