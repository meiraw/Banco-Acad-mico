import { Navigate } from 'react-router-dom'
import { useSessao } from '../lib/sessao.jsx'

export function RequireAuth({ children, admin = false }) {
  const { perfil } = useSessao()
  if (!perfil) return <Navigate to="/auth" replace />
  if (admin && perfil.perfil !== 'ADMIN') return <Navigate to="/painel" replace />
  return children
}
