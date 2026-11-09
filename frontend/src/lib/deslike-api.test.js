import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('registrar e remover deslike usam autenticação e preservam os totais retornados', async t => {
  const originalFetch = globalThis.fetch, originalStorage = globalThis.sessionStorage
  t.after(() => { globalThis.fetch = originalFetch; globalThis.sessionStorage = originalStorage })
  globalThis.sessionStorage = { getItem: () => 'token-teste' }
  const chamadas = []
  globalThis.fetch = async (url, options) => {
    chamadas.push({ url, ...options })
    return { ok: true, status: 200, json: async () => ({ total: 2, curtido: false,
      totalDeslikes: options.method === 'PUT' ? 1 : 0, deslike: options.method === 'PUT' }) }
  }
  assert.deepEqual(await api.registrarDeslike('material'), { total: 2, curtido: false, totalDeslikes: 1, deslike: true })
  assert.equal((await api.removerDeslike('material')).deslike, false)
  assert.deepEqual(chamadas.map(c => c.method), ['PUT', 'DELETE'])
  for (const chamada of chamadas) {
    assert.equal(chamada.url, '/api/material/material/deslike')
    assert.equal(chamada.headers.Authorization, 'Bearer token-teste')
  }
})
