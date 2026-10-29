import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('cadastro usa endpoint público e trata respostas da API', async t => {
  const fetchOriginal = globalThis.fetch
  t.after(() => { globalThis.fetch = fetchOriginal })
  const dados = { nome: 'Maria Silva', email: 'maria@gmail.com', semestreAtual: 2, senha: 'Senha123!' }

  await t.test('envia dados acadêmicos sem Authorization e lê sucesso', async () => {
    globalThis.fetch = async (url, options) => {
      assert.equal(url, '/api/usuario')
      assert.equal(options.method, 'POST')
      assert.equal(options.headers.Authorization, undefined)
      assert.deepEqual(JSON.parse(options.body), dados)
      assert.equal(JSON.parse(options.body).confirmacaoSenha, undefined)
      return { ok: true, status: 201, json: async () => ({ nome: dados.nome, curso: 'Ciência da Computação', semestreAtual: 2 }) }
    }
    assert.equal((await api.cadastrar(dados)).semestreAtual, 2)
  })
  await t.test('preserva status e mensagem para e-mail duplicado, validação e erro interno', async () => {
    for (const status of [400, 409, 500]) {
      globalThis.fetch = async () => ({ ok: false, status, json: async () => ({ mensagem: 'Mensagem de teste' }) })
      await assert.rejects(api.cadastrar(dados), error => error.status === status && error.message === 'Mensagem de teste')
    }
  })
  await t.test('falha de conexão é apresentada com mensagem clara', async () => {
    globalThis.fetch = async () => { throw new TypeError('Falha de rede') }
    await assert.rejects(api.cadastrar(dados), /Não foi possível conectar ao servidor/)
  })
})
