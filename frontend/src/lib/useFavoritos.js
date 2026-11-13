import { useEffect, useSyncExternalStore } from 'react'
import { useSessao } from './sessao.jsx'
import { api } from './api.js'
import { criarFavoritos } from './favoritos.js'

let sessaoAtual = null
let storeAtual = null
const vazio = { ids: new Set(), pendentes: new Set(), pronto: false, carregando: true, erro: '', revisao: 0 }
const semSessao = { snapshot: () => vazio, assinar: () => () => {}, carregar: async () => {}, alternar: async () => {}, invalidar: () => {} }

function storeDaSessao(usuarioId, token) {
  const chave = usuarioId && token ? `${usuarioId}:${token}` : null
  if (chave !== sessaoAtual) {
    storeAtual?.descartar()
    sessaoAtual = chave
    storeAtual = chave ? criarFavoritos({ api,
      estaAtiva: () => sessionStorage.getItem('banco-academico:token') === token }) : null
  }
  return storeAtual || semSessao
}

export function useFavoritos() {
  const { perfil } = useSessao()
  const store = storeDaSessao(perfil?.id, perfil?.token)
  const estado = useSyncExternalStore(store.assinar, store.snapshot, store.snapshot)
  useEffect(() => { void store.carregar() }, [store])
  return {
    isFavorito: id => estado.ids.has(id),
    alternar: store.alternar,
    disponivel: !!perfil?.id && estado.pronto,
    lista: estado.ids,
    carregando: estado.carregando,
    pendente: id => estado.pendentes.has(id),
    erro: estado.erro,
    revisao: estado.revisao,
    recarregar: store.carregar,
    invalidar: store.invalidar,
  }
}
