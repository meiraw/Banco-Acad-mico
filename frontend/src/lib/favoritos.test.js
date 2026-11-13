import test from 'node:test'
import assert from 'node:assert/strict'
import { criarFavoritos } from './favoritos.js'

const id = n => `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`
function servidor() {
  const banco = new Map()
  const chamadas = []
  const api = usuario => {
    if (!banco.has(usuario)) banco.set(usuario, new Set())
    return {
      favoritosIds: async pagina => {
        chamadas.push(['GET', usuario, pagina])
        const ids = [...banco.get(usuario)]
        return { content: ids.slice(pagina * 2, pagina * 2 + 2), totalPages: Math.ceil(ids.length / 2) }
      },
      adicionarFavorito: async material => { chamadas.push(['PUT', usuario, material]); banco.get(usuario).add(material) },
      removerFavorito: async material => { chamadas.push(['DELETE', usuario, material]); banco.get(usuario).delete(material) },
    }
  }
  return { banco, chamadas, api }
}
const adiar = () => { let resolver; const promise = new Promise(resolve => { resolver = resolve }); return { promise, resolver } }

test('persiste via API entre clientes e sessões, mantendo contas separadas', async () => {
  const s = servidor()
  const a = criarFavoritos({ api: s.api('a') })
  const b = criarFavoritos({ api: s.api('b') })
  await Promise.all([a.carregar(), b.carregar()])
  await a.alternar(id(1))
  assert.equal(a.snapshot().ids.has(id(1)), true)
  assert.equal(b.snapshot().ids.has(id(1)), false)
  a.descartar()
  const outroDispositivo = criarFavoritos({ api: s.api('a') })
  await outroDispositivo.carregar()
  assert.equal(outroDispositivo.snapshot().ids.has(id(1)), true)
  await outroDispositivo.alternar(id(1))
  assert.equal(s.banco.get('a').size, 0)
})

test('carrega todas as páginas de IDs e compartilha uma consulta simultânea', async () => {
  const s = servidor(), api = s.api('a')
  for (let n = 1; n <= 5; n++) s.banco.get('a').add(id(n))
  const store = criarFavoritos({ api })
  const primeira = store.carregar()
  assert.equal(store.carregar(), primeira)
  await primeira
  assert.equal(store.snapshot().ids.size, 5)
  assert.deepEqual(s.chamadas.filter(c => c[0] === 'GET').map(c => c[2]), [0, 1, 2])
})

test('cliques repetidos aguardam a confirmação do servidor e não duplicam requisições', async () => {
  const s = servidor(), api = s.api('a'), espera = adiar()
  let gravacoes = 0
  api.adicionarFavorito = async () => { gravacoes++; await espera.promise }
  const store = criarFavoritos({ api }); await store.carregar()
  const primeira = store.alternar(id(1))
  await store.alternar(id(1))
  assert.equal(gravacoes, 1)
  assert.equal(store.snapshot().ids.has(id(1)), false)
  assert.equal(store.snapshot().pendentes.has(id(1)), true)
  espera.resolver(); await primeira
  assert.equal(store.snapshot().ids.has(id(1)), true)
  assert.equal(store.snapshot().pendentes.size, 0)
})

test('falhas não confirmam alteração e uma nova tentativa usa PUT idempotente', async () => {
  const s = servidor(), api = s.api('a')
  let primeira = true
  api.adicionarFavorito = async material => {
    s.banco.get('a').add(material)
    if (primeira) { primeira = false; throw new Error('Falha de conexão') }
  }
  const store = criarFavoritos({ api }); await store.carregar()
  await assert.rejects(store.alternar(id(1)), /Falha de conexão/)
  assert.equal(store.snapshot().ids.has(id(1)), false)
  assert.equal(store.snapshot().pendentes.size, 0)
  await store.alternar(id(1))
  assert.equal(store.snapshot().ids.has(id(1)), true)
  assert.equal(s.banco.get('a').size, 1)
})

test('respostas atrasadas são descartadas ao trocar a conta ou sair', async () => {
  const espera = adiar()
  let ativa = true
  const store = criarFavoritos({ estaAtiva: () => ativa, api: { favoritosIds: () => espera.promise } })
  const consulta = store.carregar()
  ativa = false; store.descartar()
  espera.resolver({ content: [id(1)], totalPages: 1 }); await consulta
  assert.equal(store.snapshot().ids.size, 0)
  await assert.rejects(store.alternar(id(1)), /sessão mudou/)
})

test('consulta antiga não sobrescreve uma inclusão confirmada durante a atualização', async () => {
  const s = servidor(), api = s.api('a'), espera = adiar()
  const store = criarFavoritos({ api }); await store.carregar()
  const consultar = api.favoritosIds
  let atrasar = true
  api.favoritosIds = pagina => { if (atrasar) { atrasar = false; return espera.promise } return consultar(pagina) }
  const consulta = store.carregar()
  await store.alternar(id(1))
  espera.resolver({ content: [], totalPages: 0 }); await consulta
  await store.carregar()
  assert.equal(store.snapshot().ids.has(id(1)), true)
})

test('favoritos não acessam armazenamento local nem oferecem importação', async () => {
  const anterior = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, get() { throw new Error('Não deve acessar favoritos antigos') } })
  try {
    const s = servidor()
    const store = criarFavoritos({ api: s.api('a') })
    await store.carregar(); await store.alternar(id(1))
    assert.equal(store.snapshot().ids.has(id(1)), true)
    assert.equal('importar' in store, false)
    s.banco.get('a').delete(id(1))
    store.invalidar(id(1))
    await store.carregar()
    assert.equal(store.snapshot().ids.has(id(1)), false)
    assert.equal(s.chamadas.filter(c => c[0] === 'PUT').length, 1)
  } finally {
    if (anterior) Object.defineProperty(globalThis, 'localStorage', anterior)
    else delete globalThis.localStorage
  }
})

test('uma falha de consulta é apresentada e pode ser recuperada', async () => {
  const s = servidor(), api = s.api('a'), consultar = api.favoritosIds
  api.favoritosIds = async () => { throw new Error('Servidor indisponível') }
  const store = criarFavoritos({ api }); await store.carregar()
  assert.equal(store.snapshot().pronto, false)
  assert.equal(store.snapshot().erro, 'Servidor indisponível')
  api.favoritosIds = consultar; await store.carregar()
  assert.equal(store.snapshot().pronto, true)
  assert.equal(store.snapshot().erro, '')
})
