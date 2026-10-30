import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('perfil e fotos usam autenticação e preservam o multipart', async t => {
  const fetchOriginal = globalThis.fetch
  const storageOriginal = globalThis.sessionStorage
  globalThis.sessionStorage = { getItem: () => 'token-de-teste' }
  t.after(() => {
    globalThis.fetch = fetchOriginal
    if (storageOriginal === undefined) delete globalThis.sessionStorage
    else globalThis.sessionStorage = storageOriginal
  })
  await t.test('busca os dados da própria conta', async () => {
    globalThis.fetch = async (url, options) => {
      assert.equal(url, '/api/perfil')
      assert.equal(options.headers.Authorization, 'Bearer token-de-teste')
      return { ok: true, status: 200, json: async () => ({ nome: 'Maria', possuiFoto: true }) }
    }
    assert.equal((await api.meuPerfil()).possuiFoto, true)
  })
  await t.test('carrega publicações próprias com autenticação e paginação', async () => {
    globalThis.fetch = async (url, options) => {
      assert.equal(url, '/api/perfil/publicacoes?pagina=2')
      assert.equal(options.headers.Authorization, 'Bearer token-de-teste')
      return { ok: true, status: 200, json: async () => ({ content: [], last: true }) }
    }
    assert.deepEqual((await api.minhasPublicacoes(2)).content, [])
  })
  await t.test('baixa foto autenticada como blob', async () => {
    const foto = new Blob(['imagem'], { type: 'image/png' })
    globalThis.fetch = async (url, options) => {
      assert.equal(url, '/api/perfil/foto')
      assert.equal(options.headers.Authorization, 'Bearer token-de-teste')
      return { ok: true, status: 200, blob: async () => foto }
    }
    assert.equal(await api.fotoPerfil(), foto)
  })
  await t.test('envia arquivo sem fixar boundary ou escolher outra conta', async () => {
    globalThis.fetch = async (url, options) => {
      assert.equal(url, '/api/perfil/foto')
      assert.equal(options.method, 'PUT')
      assert.equal(options.headers.Authorization, 'Bearer token-de-teste')
      assert.equal(options.headers['Content-Type'], undefined)
      assert.ok(options.body.get('foto') instanceof Blob)
      assert.deepEqual([...options.body.keys()], ['foto'])
      return { ok: true, status: 200, json: async () => ({ mensagem: 'Foto atualizada' }) }
    }
    await api.salvarFotoPerfil(new Blob(['imagem'], { type: 'image/png' }))
  })
})
