import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('publicação carrega detalhes, pagina comentários e publica com identidade autenticada', async t => {
  const fetchOriginal = globalThis.fetch
  const storageOriginal = globalThis.sessionStorage
  globalThis.sessionStorage = { getItem: () => 'token-teste' }
  t.after(() => {
    globalThis.fetch = fetchOriginal
    if (storageOriginal === undefined) delete globalThis.sessionStorage
    else globalThis.sessionStorage = storageOriginal
  })
  const chamadas = []
  globalThis.fetch = async (url, options) => {
    chamadas.push({ url, options })
    return { ok: true, status: options.method === 'POST' ? 201 : 200, json: async () => ({ id: 'material' }) }
  }
  await api.material('material')
  await api.comentarios('material', 2)
  await api.comentar('material', 'Uma contribuição')
  assert.deepEqual(chamadas.map(c => c.url), ['/api/material/material', '/api/material/material/comentarios?pagina=2', '/api/material/material/comentarios'])
  assert.equal(chamadas[2].options.method, 'POST')
  assert.equal(chamadas[2].options.headers.Authorization, 'Bearer token-teste')
  assert.deepEqual(JSON.parse(chamadas[2].options.body), { texto: 'Uma contribuição' })
  await api.curtida('material')
  await api.curtir('material')
  await api.descurtir('material')
  await api.respostas('material', 'comentario', 1)
  await api.responder('material', 'comentario', 'Minha resposta')
  assert.equal(chamadas[3].url, '/api/material/material/curtida')
  assert.equal(chamadas[4].options.method, 'PUT')
  assert.equal(chamadas[5].options.method, 'DELETE')
  assert.equal(chamadas[6].url, '/api/material/material/comentarios/comentario/respostas?pagina=1')
  assert.equal(chamadas[7].options.headers.Authorization, 'Bearer token-teste')
  assert.equal(chamadas[7].options.method, 'POST')
  assert.deepEqual(JSON.parse(chamadas[7].options.body), { texto: 'Minha resposta' })
  await api.responder('material', 'comentario', 'Respondendo ao colega', 'resposta-do-colega')
  assert.deepEqual(JSON.parse(chamadas[8].options.body), { texto: 'Respondendo ao colega', respostaPaiId: 'resposta-do-colega' })
  globalThis.fetch = async () => ({ ok: false, status: 404, json: async () => ({ mensagem: 'Material não encontrado.' }) })
  await assert.rejects(api.material('ausente'), e => e.status === 404 && e.message === 'Material não encontrado.')
})
