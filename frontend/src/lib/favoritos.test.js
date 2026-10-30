import test from 'node:test'
import assert from 'node:assert/strict'
import { alternarFavorito, chaveFavoritos, idsFavoritos, lerFavoritos } from './favoritos.js'

test('favoritos de ADMIN e USER ficam separados ao alternar contas no mesmo navegador', t => {
  const storageOriginal = globalThis.localStorage
  const windowOriginal = globalThis.window
  const customEventOriginal = globalThis.CustomEvent
  const dados = new Map([['banco-academico:favoritos', '["publicacao-antiga"]']])
  globalThis.localStorage = { getItem: chave => dados.get(chave) ?? null, setItem: (chave, valor) => dados.set(chave, valor) }
  globalThis.window = new EventTarget()
  if (!globalThis.CustomEvent) globalThis.CustomEvent = class extends Event {
    constructor(tipo, options) { super(tipo); this.detail = options.detail }
  }
  t.after(() => {
    for (const [nome, original] of [['localStorage', storageOriginal], ['window', windowOriginal], ['CustomEvent', customEventOriginal]]) {
      if (original === undefined) delete globalThis[nome]
      else globalThis[nome] = original
    }
  })
  const lista = usuario => [...idsFavoritos(lerFavoritos(usuario))]
  const eventos = []
  window.addEventListener('favoritos-atualizados', event => eventos.push(event.detail.chave))

  assert.deepEqual(lista('admin-id'), [])
  assert.deepEqual(lista('user-id'), [])
  alternarFavorito('admin-id', 'publicacao-1')
  assert.deepEqual(lista('admin-id'), ['publicacao-1'])
  assert.deepEqual(lista('user-id'), [])
  alternarFavorito('user-id', 'publicacao-2')
  assert.deepEqual(lista('user-id'), ['publicacao-2'])
  assert.deepEqual(lista('admin-id'), ['publicacao-1'])
  alternarFavorito('user-id', 'publicacao-1')
  alternarFavorito('admin-id', 'publicacao-1')
  assert.deepEqual(lista('admin-id'), [])
  assert.deepEqual(lista('user-id'), ['publicacao-2', 'publicacao-1'])
  assert.deepEqual(lista('outro-user-id'), [])
  alternarFavorito('outro-user-id', 'publicacao-3')
  assert.deepEqual(lista('outro-user-id'), ['publicacao-3'])
  assert.deepEqual(lista('user-id'), ['publicacao-2', 'publicacao-1'])
  assert.deepEqual(lista('admin-id'), [])
  assert.equal(eventos[0], chaveFavoritos('admin-id'))
  assert.equal(eventos[1], chaveFavoritos('user-id'))

  const antes = [...dados.entries()]
  alternarFavorito(undefined, 'publicacao-3')
  assert.deepEqual([...dados.entries()], antes)
  assert.deepEqual(lista(undefined), [])
  assert.equal(dados.get('banco-academico:favoritos'), '["publicacao-antiga"]')
  dados.set(chaveFavoritos('corrompido'), '{invalido')
  assert.deepEqual(lista('corrompido'), [])
})
