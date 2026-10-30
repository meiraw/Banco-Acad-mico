import test from 'node:test'
import assert from 'node:assert/strict'
import { alternarTema, chaveTema, lerTema } from './tema.js'

test('tema é individual para ADMIN, dois USERs e visitante, incluindo troca e retorno à conta', t => {
  const originais = { localStorage: globalThis.localStorage, window: globalThis.window, CustomEvent: globalThis.CustomEvent }
  const dados = new Map([['banco-academico:tema', 'escuro']])
  globalThis.localStorage = { getItem: chave => dados.get(chave) ?? null, setItem: (chave, valor) => dados.set(chave, valor) }
  globalThis.window = new EventTarget()
  if (!globalThis.CustomEvent) globalThis.CustomEvent = class extends Event {
    constructor(tipo, options) { super(tipo); this.detail = options.detail }
  }
  t.after(() => {
    for (const [nome, valor] of Object.entries(originais)) {
      if (valor === undefined) delete globalThis[nome]
      else globalThis[nome] = valor
    }
  })
  const eventos = []
  window.addEventListener('tema-atualizado', event => eventos.push(event.detail.chave))
  assert.equal(lerTema('admin-id'), 'claro')
  alternarTema('user-1')
  assert.equal(lerTema('user-1'), 'escuro')
  assert.equal(lerTema('admin-id'), 'claro')
  assert.equal(lerTema('user-2'), 'claro')
  assert.equal(lerTema('visitante'), 'claro')
  alternarTema('admin-id')
  alternarTema('user-1')
  assert.equal(lerTema('user-1'), 'claro')
  assert.equal(lerTema('admin-id'), 'escuro')
  assert.equal(lerTema('user-2'), 'claro')
  alternarTema('visitante')
  assert.equal(lerTema('user-2'), 'claro')
  assert.equal(lerTema('admin-id'), 'escuro')
  assert.equal(lerTema('user-1'), 'claro')
  assert.equal(eventos[0], chaveTema('user-1'))
  assert.equal(eventos[1], chaveTema('admin-id'))
  const antes = [...dados.entries()]
  alternarTema(null)
  assert.deepEqual([...dados.entries()], antes)
  assert.equal(lerTema(null), 'claro')
  assert.equal(dados.get('banco-academico:tema'), 'escuro')
})
