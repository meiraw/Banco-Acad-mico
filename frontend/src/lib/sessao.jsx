import { createContext, useContext, useEffect, useState } from 'react'

const CHAVE = 'banco-academico:sessao'
const TOKEN = 'banco-academico:token'
const SessaoContexto = createContext(null)

function claims(token) {
  const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
  return JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(payload), c => c.charCodeAt(0))))
}
function carregar() {
  try {
    const token = sessionStorage.getItem(TOKEN)
    if (!token || claims(token).exp * 1000 <= Date.now()) return null
    return { ...JSON.parse(sessionStorage.getItem(CHAVE)), perfil: claims(token).perfil, token }
  } catch { return null }
}

export function SessaoProvider({ children }) {
  const [perfil, setPerfil] = useState(carregar)
  function entrar(dados) {
    const info = claims(dados.token)
    const sessao = { ...dados, perfil: info.perfil }
    sessionStorage.setItem(TOKEN, dados.token)
    sessionStorage.setItem(CHAVE, JSON.stringify(sessao))
    setPerfil(sessao)
  }
  function sair() {
    sessionStorage.removeItem(CHAVE)
    sessionStorage.removeItem(TOKEN)
    setPerfil(null)
  }
  useEffect(() => {
    localStorage.removeItem(CHAVE) // Descarta a sessao antiga do prototipo.
    if (!perfil) { sessionStorage.removeItem(TOKEN); return }
    const tempo = claims(perfil.token).exp * 1000 - Date.now()
    const timer = setTimeout(sair, Math.max(0, tempo))
    return () => clearTimeout(timer)
  }, [perfil])
  return <SessaoContexto.Provider value={{ perfil, entrar, sair }}>{children}</SessaoContexto.Provider>
}

export function useSessao() {
  const ctx = useContext(SessaoContexto)
  if (!ctx) throw new Error('useSessao precisa estar dentro de <SessaoProvider>')
  return ctx
}
