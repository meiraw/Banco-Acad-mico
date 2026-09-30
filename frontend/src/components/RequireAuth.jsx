import { Navigate } from 'react-router-dom'
import { useSessao } from '../lib/sessao.jsx'

export function RequireAuth({ children }) {
  const { perfil } = useSessao()
  if (!perfil) return <Navigate to="/auth" replace />
  return children
}
