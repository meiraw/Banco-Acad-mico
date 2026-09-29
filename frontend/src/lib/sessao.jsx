import { createContext, useContext, useEffect, useState } from 'react'

// AVISO: o backend Spring Boot ainda não expõe cadastro/login (SecurityConfig libera tudo).
// Esta "sessão" fica só no navegador, para dar a experiência de login do protótipo
// enquanto o back-end real de autenticação não existe. Nenhum dado é enviado ao servidor.
const CHAVE = 'banco-academico:sessao'
const SessaoContexto = createContext(null)

export function SessaoProvider({ children }) {
  const [perfil, setPerfil] = useState(() => {
    try { return JSON.parse(localStorage.getItem(CHAVE)) } catch { return null }
  })

  function entrar(dados) {
    localStorage.setItem(CHAVE, JSON.stringify(dados))
    setPerfil(dados)
  }
  function sair() {
    localStorage.removeItem(CHAVE)
    setPerfil(null)
  }

  return <SessaoContexto.Provider value={{ perfil, entrar, sair }}>{children}</SessaoContexto.Provider>
}

export function useSessao() {
  const ctx = useContext(SessaoContexto)
  if (!ctx) throw new Error('useSessao precisa estar dentro de <SessaoProvider>')
  return ctx
}
