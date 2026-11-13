import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('edição de perfil e senha usam a própria conta com Bearer, sem ID na URL', async t => {
  const original = { fetch: globalThis.fetch, sessionStorage: globalThis.sessionStorage }
  t.after(() => { Object.assign(globalThis, original) })
  globalThis.sessionStorage = { getItem: () => 'token-teste' }
  const chamadas = []
  globalThis.fetch = async (url, options) => { chamadas.push({ url, options }); return { ok: true, status: 200, json: async () => ({ mensagem: 'OK' }) } }
  await api.atualizarPerfil({ nome: 'Maria', semestreAtual: 4 })
  await api.alterarSenha({ senhaAtual: 'Ficticia123!', novaSenha: ' NovaFicticia123! ', confirmacaoNovaSenha: ' NovaFicticia123! ' })
  assert.deepEqual(chamadas.map(c => c.url), ['/api/perfil', '/api/perfil/senha'])
  for (const { options } of chamadas) { assert.equal(options.method, 'PUT'); assert.equal(options.headers.Authorization, 'Bearer token-teste'); assert.equal(options.headers['Content-Type'], 'application/json') }
  assert.deepEqual(JSON.parse(chamadas[0].options.body), { semestreAtual: 4 })
  assert.equal(JSON.parse(chamadas[1].options.body).novaSenha, ' NovaFicticia123! ')
  globalThis.fetch = async () => ({ ok: false, status: 400, json: async () => ({ mensagem: 'Senha atual incorreta.' }) })
  await assert.rejects(api.alterarSenha({}), e => e.status === 400 && e.message === 'Senha atual incorreta.')
  globalThis.fetch = async () => ({ ok: false, status: 429, json: async () => ({ mensagem: 'Muitas tentativas.' }) })
  await assert.rejects(api.alterarSenha({}), e => e.status === 429)
})
