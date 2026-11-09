import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('exclusão de publicação envia DELETE autenticado e respeita a resposta sem corpo', async t => {
  const originalFetch = globalThis.fetch
  const originalStorage = globalThis.sessionStorage
  t.after(() => { globalThis.fetch = originalFetch; globalThis.sessionStorage = originalStorage })
  globalThis.sessionStorage = { getItem: () => 'token-do-autor' }
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/material/material-proprio')
    assert.equal(options.method, 'DELETE')
    assert.equal(options.headers.Authorization, 'Bearer token-do-autor')
    return { ok: true, status: 204 }
  }
  assert.equal(await api.excluirMaterial('material-proprio'), null)
  globalThis.fetch = async () => ({ ok: false, status: 403, json: async () => ({ mensagem: 'Você só pode excluir suas próprias publicações.' }) })
  await assert.rejects(api.excluirMaterial('material-alheio'), e => e.status === 403 && /próprias/.test(e.message))
})
