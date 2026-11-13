import test from 'node:test'
import assert from 'node:assert/strict'
import { TIPOS_PUBLICACAO, dadosPublicacao, rotuloTipoPublicacao } from './tipoPublicacao.js'
import { api } from './api.js'

test('tipos válidos, referência obrigatória para Artigo e opcional para Livro', () => {
  assert.equal(TIPOS_PUBLICACAO.length, 7)
  assert.equal(rotuloTipoPublicacao(null), 'Não informado')
  assert.throws(() => dadosPublicacao('', ''), /Selecione/)
  assert.throws(() => dadosPublicacao('PDF', ''), /Selecione/)
  for (const [tipo, rotulo] of TIPOS_PUBLICACAO) {
    assert.equal(rotuloTipoPublicacao(tipo), rotulo)
    assert.deepEqual(dadosPublicacao(tipo, ' Referência anterior '), {
      tipoPublicacao: tipo, referenciaBibliografica: ['LIVRO', 'ARTIGO'].includes(tipo) ? 'Referência anterior' : null,
    })
  }
  assert.equal(dadosPublicacao('LIVRO', '   ').referenciaBibliografica, null)
  for (const referencia of [undefined, null, '', '   ', '\t\n']) {
    assert.throws(() => dadosPublicacao('ARTIGO', referencia), /Informe a referência bibliográfica do artigo/)
    assert.equal(dadosPublicacao('LIVRO', referencia).referenciaBibliografica, null)
    for (const [tipo] of TIPOS_PUBLICACAO.filter(([valor]) => !['LIVRO', 'ARTIGO'].includes(valor))) {
      assert.equal(dadosPublicacao(tipo, referencia).referenciaBibliografica, null)
    }
  }
  assert.equal(dadosPublicacao('ARTIGO', ' Autor. Artigo. 2026. ').referenciaBibliografica, 'Autor. Artigo. 2026.')
})

test('upload preserva arquivo multipart e envia tipo e referência em JSON separado do MIME', async t => {
  const originalFetch = globalThis.fetch
  const originalStorage = globalThis.sessionStorage
  t.after(() => { globalThis.fetch = originalFetch; globalThis.sessionStorage = originalStorage })
  globalThis.sessionStorage = { getItem: () => 'token-teste' }
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/material')
    assert.equal(options.headers.Authorization, 'Bearer token-teste')
    assert.equal(options.headers['Content-Type'], undefined)
    const dados = JSON.parse(await options.body.get('dados').text())
    assert.equal(dados.tipoPublicacao, 'LIVRO')
    assert.equal(dados.referenciaBibliografica, 'Autor. Título. 2026.')
    assert.equal(dados.responsabilidadeConfirmada, true)
    assert.equal(options.body.get('arquivo').type, 'application/pdf')
    assert.equal(await options.body.get('arquivo').text(), '%PDF')
    return { ok: true, status: 201, json: async () => ({ id: 'material', ...dados, statusModeracao: 'PENDENTE' }) }
  }
  const resultado = await api.criarMaterial({ titulo: 'Livro', descricao: 'Descrição', disciplinaId: 'disciplina', responsabilidadeConfirmada: true,
    ...dadosPublicacao('LIVRO', 'Autor. Título. 2026.') }, new Blob(['%PDF'], { type: 'application/pdf' }))
  assert.equal(resultado.statusModeracao, 'PENDENTE')
})
