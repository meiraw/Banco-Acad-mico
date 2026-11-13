import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { SessaoProvider, useSessao } from './lib/sessao.jsx'
import { RequireAuth } from './components/RequireAuth.jsx'
import { Home } from './pages/Home.jsx'
import { Auth } from './pages/Auth.jsx'
import { ConfirmarEmail } from './pages/ConfirmarEmail.jsx'
import { RedefinirSenha } from './pages/RedefinirSenha.jsx'
import { Painel } from './pages/Painel.jsx'
import { Compartilhar } from './pages/Compartilhar.jsx'
import { Favoritos } from './pages/Favoritos.jsx'
import { Perfil } from './pages/Perfil.jsx'
import { Sobre } from './pages/Sobre.jsx'
import { Termos } from './pages/Termos.jsx'
import { Publicacao } from './pages/Publicacao.jsx'
import { Configuracoes } from './pages/Configuracoes.jsx'
import { TemaProvider } from './lib/useTema.js'
import { Forum } from './pages/Forum.jsx'
import { NovaDiscussao } from './pages/NovaDiscussao.jsx'
import { Discussao } from './pages/Discussao.jsx'
import { EditarPerfil } from './pages/EditarPerfil.jsx'
import { AlterarSenha } from './pages/AlterarSenha.jsx'
import { Privacidade } from './pages/Privacidade.jsx'
import { Ajuda } from './pages/Ajuda.jsx'
import { PublicacoesDoUsuario } from './pages/PublicacoesDoUsuario.jsx'

export default function App() {
  return <SessaoProvider><InterfaceConta /></SessaoProvider>
}

function InterfaceConta() {
  const { perfil } = useSessao()
  return (
    <TemaProvider key={perfil?.token || 'visitante'}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/auth" element={<Auth key="login" />} />
          <Route path="/cadastro" element={<Auth key="cadastro" />} />
          <Route path="/confirmar-email" element={<ConfirmarEmail />} />
          <Route path="/redefinir-senha" element={<RedefinirSenha />} />
          <Route path="/sobre" element={<Sobre />} />
          <Route path="/termos" element={<Termos />} />
          <Route path="/privacidade" element={<Privacidade />} />
          <Route path="/ajuda" element={<Ajuda />} />
          <Route path="/painel" element={<RequireAuth><Painel /></RequireAuth>} />
          <Route path="/forum" element={<RequireAuth><Forum /></RequireAuth>} />
          <Route path="/forum/nova" element={<RequireAuth><NovaDiscussao /></RequireAuth>} />
          <Route path="/forum/:id" element={<RequireAuth><Discussao /></RequireAuth>} />
          <Route path="/material/:id" element={<RequireAuth><Publicacao /></RequireAuth>} />
          <Route path="/compartilhar" element={<RequireAuth><Compartilhar /></RequireAuth>} />
          <Route path="/favoritos" element={<RequireAuth><Favoritos /></RequireAuth>} />
          <Route path="/perfil" element={<RequireAuth>{perfil?.perfil === 'ADMIN' ? <Perfil /> : <Navigate to="/configuracoes/perfil" replace />}</RequireAuth>} />
          <Route path="/minhas-publicacoes" element={<RequireAuth>{perfil?.perfil === 'ADMIN' ? <Navigate to="/perfil" replace /> : <PublicacoesDoUsuario />}</RequireAuth>} />
          <Route path="/configuracoes" element={<RequireAuth><Configuracoes /></RequireAuth>} />
          <Route path="/configuracoes/perfil" element={<RequireAuth><EditarPerfil /></RequireAuth>} />
          <Route path="/configuracoes/senha" element={<RequireAuth><AlterarSenha /></RequireAuth>} />
          <Route path="/admin" element={<RequireAuth admin><Navigate to="/perfil#administracao" replace /></RequireAuth>} />
          <Route path="*" element={<Home />} />
        </Routes>
      </BrowserRouter>
    </TemaProvider>
  )
}
