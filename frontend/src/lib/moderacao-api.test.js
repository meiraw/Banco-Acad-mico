import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('moderação usa Bearer e PATCH e busca o arquivo com autenticação', async () => {
  const originalFetch = globalThis.fetch
  const originalStorage = globalThis.sessionStorage
  const chamadas = []
  globalThis.sessionStorage = { getItem: () => 'token-de-teste' }
  globalThis.fetch = async (url, opcoes) => {
    chamadas.push({ url, ...opcoes })
    return { ok: true, status: 200, json: async () => ({}), blob: async () => new Blob(['%PDF']) }
  }
  try {
    await api.materiaisPendentes(2)
    await api.aprovarMaterial('id')
    await api.rejeitarMaterial('id', 'Autorização não identificada')
    const arquivo = await api.arquivoMaterial('id')
    assert.equal(chamadas[0].url, '/api/admin/materiais/pendentes?pagina=2')
    assert.equal(chamadas[1].method, 'PATCH')
    assert.equal(chamadas[1].url, '/api/admin/materiais/id/aprovar')
    assert.equal(chamadas[2].method, 'PATCH')
    assert.equal(chamadas[2].url, '/api/admin/materiais/id/rejeitar')
    assert.deepEqual(JSON.parse(chamadas[2].body), { justificativa: 'Autorização não identificada' })
    for (const chamada of chamadas) assert.equal(chamada.headers.Authorization, 'Bearer token-de-teste')
    assert.equal(chamadas[3].url, '/api/material/id/arquivo')
    assert.equal(await arquivo.text(), '%PDF')
  } finally {
    globalThis.fetch = originalFetch
    globalThis.sessionStorage = originalStorage
  }
})
