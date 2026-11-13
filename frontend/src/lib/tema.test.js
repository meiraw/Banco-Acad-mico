import test from 'node:test'
import assert from 'node:assert/strict'
import { alternarTema, chaveTema, lerTema, definirTema, resolverTema, assinarTemaSistema } from './tema.js'

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

test('automático persiste por conta e acompanha mudanças do dispositivo sem alterar a preferência', t => {
  const originais = { localStorage: globalThis.localStorage, window: globalThis.window, CustomEvent: globalThis.CustomEvent }
  t.after(() => { for (const [chave, valor] of Object.entries(originais)) { if (valor === undefined) delete globalThis[chave]; else globalThis[chave] = valor } })
  const dados = new Map()
  globalThis.localStorage = { getItem: chave => dados.get(chave), setItem: (chave, valor) => dados.set(chave, valor) }
  globalThis.window = new EventTarget()
  if (!globalThis.CustomEvent) globalThis.CustomEvent = class extends Event { constructor(tipo, opcoes) { super(tipo); this.detail = opcoes.detail } }
  const media = new EventTarget(); media.matches = false
  window.matchMedia = () => media
  assert.equal(definirTema('conta', 'automatico'), true)
  assert.equal(lerTema('conta'), 'automatico'); assert.equal(lerTema('outra'), 'claro')
  let tema = resolverTema(lerTema('conta'), media.matches), chamadas = 0
  const remover = assinarTemaSistema(media, () => { chamadas++; tema = resolverTema(lerTema('conta'), media.matches) })
  assert.equal(tema, 'claro')
  media.matches = true; media.dispatchEvent(new Event('change'))
  assert.equal(tema, 'escuro'); assert.equal(lerTema('conta'), 'automatico')
  media.matches = false; media.dispatchEvent(new Event('change'))
  assert.equal(tema, 'claro'); assert.equal(chamadas, 2)
  remover(); media.matches = true; media.dispatchEvent(new Event('change'))
  assert.equal(chamadas, 2)
  assert.equal(resolverTema('claro', true), 'claro'); assert.equal(resolverTema('escuro', false), 'escuro')
  alternarTema('conta', 'escuro')
  assert.equal(lerTema('conta'), 'claro')
  dados.set(chaveTema('conta'), 'invalido'); assert.equal(lerTema('conta'), 'claro')
  assert.equal(definirTema('conta', 'invalido'), false)
})

test('armazenamento indisponível e ausência de matchMedia não causam exceção', t => {
  const originais = { localStorage: globalThis.localStorage, window: globalThis.window }
  t.after(() => { for (const [chave, valor] of Object.entries(originais)) { if (valor === undefined) delete globalThis[chave]; else globalThis[chave] = valor } })
  globalThis.localStorage = { getItem() { throw new Error('bloqueado') }, setItem() { throw new Error('bloqueado') } }
  globalThis.window = new EventTarget()
  assert.equal(lerTema('conta'), 'claro'); assert.equal(definirTema('conta', 'automatico'), false)
  assert.equal(alternarTema('conta'), false)
  assert.doesNotThrow(() => assinarTemaSistema(null, () => {})())
})
