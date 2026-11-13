import { createContext, createElement, useCallback, useContext, useLayoutEffect, useMemo, useSyncExternalStore } from 'react'
import { useSessao } from './sessao.jsx'
import { alternarTema, chaveTema, lerTema, definirTema, resolverTema, assinarTemaSistema } from './tema.js'

const TemaContexto = createContext(null)

export function TemaProvider({ children }) {
  const { perfil } = useSessao()
  const conta = perfil ? perfil.id || null : 'visitante'
  const ler = useCallback(() => lerTema(conta), [conta])
  const assinar = useCallback(notificar => {
    const chave = chaveTema(conta)
    const atualizar = event => {
      if (chave && (event.key === null || event.key === chave || event.detail?.chave === chave)) notificar()
    }
    window.addEventListener('tema-atualizado', atualizar)
    window.addEventListener('storage', atualizar)
    return () => {
      window.removeEventListener('tema-atualizado', atualizar)
      window.removeEventListener('storage', atualizar)
    }
  }, [conta])
  const preferencia = useSyncExternalStore(assinar, ler, () => 'claro')
  const media = useMemo(() => window.matchMedia?.('(prefers-color-scheme: dark)'), [])
  const assinarSistema = useCallback(notificar => preferencia === 'automatico' ? assinarTemaSistema(media, notificar) : () => {}, [media, preferencia])
  const lerSistema = useCallback(() => media?.matches ?? false, [media])
  const prefereEscuro = useSyncExternalStore(assinarSistema, lerSistema, () => false)
  const tema = resolverTema(preferencia, prefereEscuro)

  useLayoutEffect(() => {
    document.documentElement.classList.toggle('dark', tema === 'escuro')
  }, [tema])

  return createElement(TemaContexto.Provider, { value: {
    tema, preferencia, selecionar: valor => definirTema(conta, valor),
    alternar: () => alternarTema(conta, tema), disponivel: !!conta,
  } }, children)
}

export function useTema() {
  const contexto = useContext(TemaContexto)
  if (!contexto) throw new Error('useTema precisa estar dentro de <TemaProvider>')
  return contexto
}
