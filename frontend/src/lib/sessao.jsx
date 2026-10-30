import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { api } from './api.js'
import { claims, carregarSessao } from './sessao-local.js'

const CHAVE = 'banco-academico:sessao'
const TOKEN = 'banco-academico:token'
const SessaoContexto = createContext(null)

export function SessaoProvider({ children }) {
  const [perfil, setPerfil] = useState(carregarSessao)
  const [fotoUrl, setFotoUrl] = useState(null)
  const [erroPerfil, setErroPerfil] = useState('')
  const consultaPerfil = useRef(0)
  const tokenAtual = perfil?.token

  const recarregarPerfil = useCallback(async () => {
    if (!tokenAtual) return
    const consulta = ++consultaPerfil.current
    const dados = await api.meuPerfil()
    if (consulta !== consultaPerfil.current || sessionStorage.getItem(TOKEN) !== tokenAtual) return
    setPerfil(atual => {
      if (!atual || atual.token !== tokenAtual) return atual
      const atualizado = { ...atual, ...dados }
      sessionStorage.setItem(CHAVE, JSON.stringify(atualizado))
      return atualizado
    })
    const foto = dados.possuiFoto ? await api.fotoPerfil() : null
    if (consulta !== consultaPerfil.current || sessionStorage.getItem(TOKEN) !== tokenAtual) return
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
    consultaPerfil.current++
    setFotoUrl(null)
    setErroPerfil('')
    const info = claims(dados.token)
    const sessao = { ...dados, email: info.sub, perfil: info.perfil }
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
