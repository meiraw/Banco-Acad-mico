import test from 'node:test'
import assert from 'node:assert/strict'
import { carregarSessao } from './sessao-local.js'

test('sessão nunca reaproveita perfil de outra conta e rejeita token expirado', t => {
  const original = globalThis.sessionStorage
  const dados = new Map()
  globalThis.sessionStorage = { getItem: chave => dados.get(chave) ?? null }
  t.after(() => {
    if (original === undefined) delete globalThis.sessionStorage
    else globalThis.sessionStorage = original
  })
  const token = info => `cabecalho.${Buffer.from(JSON.stringify(info)).toString('base64url')}.assinatura`
  const exp = Math.floor(Date.now() / 1000) + 3600
  dados.set('banco-academico:sessao', JSON.stringify({ id: 'admin-id', nome: 'Admin', email: 'admin@example.org', perfil: 'ADMIN' }))
  dados.set('banco-academico:token', token({ sub: 'user@example.org', perfil: 'USER', exp }))
  const sessao = carregarSessao()
  assert.equal(sessao.email, 'user@example.org')
  assert.equal(sessao.perfil, 'USER')
  assert.equal(sessao.id, undefined)
  assert.equal(sessao.nome, undefined)
  dados.set('banco-academico:sessao', JSON.stringify({ id: 'user-id', email: 'user@example.org', perfil: 'ADMIN' }))
  assert.equal(carregarSessao().id, 'user-id')
  assert.equal(carregarSessao().perfil, 'USER')
  dados.set('banco-academico:token', token({ sub: 'user@example.org', perfil: 'USER', exp: 1 }))
  assert.equal(carregarSessao(), null)
  dados.delete('banco-academico:token')
  assert.equal(carregarSessao(), null)
})
