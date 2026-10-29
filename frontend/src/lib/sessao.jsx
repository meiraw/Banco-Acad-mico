import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { api } from './api.js'

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
  const [fotoUrl, setFotoUrl] = useState(null)
  const [erroPerfil, setErroPerfil] = useState('')
  const consultaPerfil = useRef(0)
  const tokenAtual = perfil?.token

  const recarregarPerfil = useCallback(async () => {
    if (!tokenAtual) return
    const consulta = ++consultaPerfil.current
    const dados = await api.meuPerfil()
    const foto = dados.possuiFoto ? await api.fotoPerfil() : null
    if (consulta !== consultaPerfil.current || sessionStorage.getItem(TOKEN) !== tokenAtual) return
    setPerfil(atual => {
      if (!atual || atual.token !== tokenAtual) return atual
      const atualizado = { ...atual, ...dados }
      sessionStorage.setItem(CHAVE, JSON.stringify(atualizado))
      return atualizado
    })
    setFotoUrl(foto ? URL.createObjectURL(foto) : null)
    setErroPerfil('')
  }, [tokenAtual])

  useEffect(() => {
    if (!tokenAtual) return
    let ativo = true
    recarregarPerfil().catch(error => { if (ativo) setErroPerfil(error.message) })
    return () => { ativo = false; consultaPerfil.current++ }
  }, [tokenAtual, recarregarPerfil])

  useEffect(() => () => { if (fotoUrl) URL.revokeObjectURL(fotoUrl) }, [fotoUrl])
  function entrar(dados) {
    setFotoUrl(null)
    setErroPerfil('')
    const info = claims(dados.token)
    const sessao = { ...dados, perfil: info.perfil }
    sessionStorage.setItem(TOKEN, dados.token)
    sessionStorage.setItem(CHAVE, JSON.stringify(sessao))
    setPerfil(sessao)
  }
  function sair() {
    consultaPerfil.current++
    setFotoUrl(null)
    setErroPerfil('')
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
  return <SessaoContexto.Provider value={{ perfil, fotoUrl, erroPerfil, recarregarPerfil, entrar, sair }}>{children}</SessaoContexto.Provider>
}

export function useSessao() {
  const ctx = useContext(SessaoContexto)
  if (!ctx) throw new Error('useSessao precisa estar dentro de <SessaoProvider>')
  return ctx
}
