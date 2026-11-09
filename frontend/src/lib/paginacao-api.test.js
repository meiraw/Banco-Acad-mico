import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

test('VIA_DTO preserva totais e navegação da fila e das publicações próprias', async t => {
  const originalFetch = globalThis.fetch, originalStorage = globalThis.sessionStorage
  t.after(() => { globalThis.fetch = originalFetch; globalThis.sessionStorage = originalStorage })
  globalThis.sessionStorage = { getItem: () => 'token-teste' }
  const casos = [
    { number: 0, totalElements: 25, totalPages: 3, first: true, last: false },
    { number: 1, totalElements: 25, totalPages: 3, first: false, last: false },
    { number: 2, totalElements: 25, totalPages: 3, first: false, last: true },
    { number: 0, totalElements: 0, totalPages: 0, first: true, last: true },
    { number: 3, totalElements: 25, totalPages: 3, first: false, last: true },
  ]
  for (const [consulta, caminho] of [[api.minhasPublicacoes, '/api/perfil/publicacoes'], [api.materiaisPendentes, '/api/admin/materiais/pendentes']]) {
    for (const caso of casos) {
      const content = caso.totalElements ? [{ id: 'material', titulo: 'Resumo', disciplinaId: 'disciplina' }] : []
      globalThis.fetch = async (url, opcoes) => {
        assert.equal(url, `${caminho}?pagina=${caso.number}`)
        assert.equal(opcoes.headers.Authorization, 'Bearer token-teste')
        return { ok: true, status: 200, json: async () => ({ content,
          page: { size: 12, number: caso.number, totalElements: caso.totalElements, totalPages: caso.totalPages } }) }
      }
      const resultado = await consulta(caso.number)
      assert.deepEqual(resultado, { content, size: 12, ...caso })
      assert.equal(resultado.content, content)
    }
  }
})

test('catálogos mantêm content e size=500 para as buscas e filtros existentes', async t => {
  const originalFetch = globalThis.fetch, originalStorage = globalThis.sessionStorage
  t.after(() => { globalThis.fetch = originalFetch; globalThis.sessionStorage = originalStorage })
  globalThis.sessionStorage = { getItem: () => null }
  const content = [{ id: 'material', titulo: 'Resumo', disciplinaId: 'disciplina', tipoArquivo: 'application/pdf' }]
  for (const [consulta, caminho] of [[api.materiais, '/api/material'], [api.disciplinas, '/api/disciplinas']]) {
    globalThis.fetch = async url => {
      assert.equal(url, `${caminho}?size=500`)
      return { ok: true, status: 200, json: async () => ({ content, page: { size: 500, number: 0, totalElements: 1, totalPages: 1 } }) }
    }
    assert.equal(await consulta(), content)
  }
})

test('resposta antiga continua aceita durante o reinício do backend', async t => {
  const originalFetch = globalThis.fetch, originalStorage = globalThis.sessionStorage
  t.after(() => { globalThis.fetch = originalFetch; globalThis.sessionStorage = originalStorage })
  globalThis.sessionStorage = { getItem: () => 'token-teste' }
  const resposta = { content: [], number: 0, size: 12, totalElements: 0, totalPages: 0, first: true, last: true }
  globalThis.fetch = async () => ({ ok: true, status: 200, json: async () => resposta })
  assert.equal(await api.minhasPublicacoes(), resposta)
})
