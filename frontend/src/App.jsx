import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { SessaoProvider } from './lib/sessao.jsx'
import { RequireAuth } from './components/RequireAuth.jsx'
import { Home } from './pages/Home.jsx'
import { Auth } from './pages/Auth.jsx'
import { Painel } from './pages/Painel.jsx'
import { Compartilhar } from './pages/Compartilhar.jsx'
import { Favoritos } from './pages/Favoritos.jsx'
import { Perfil } from './pages/Perfil.jsx'
import { Sobre } from './pages/Sobre.jsx'
import { Termos } from './pages/Termos.jsx'

export default function App() {
  return (
    <SessaoProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/auth" element={<Auth />} />
          <Route path="/sobre" element={<Sobre />} />
          <Route path="/termos" element={<Termos />} />
          <Route path="/painel" element={<RequireAuth><Painel /></RequireAuth>} />
          <Route path="/compartilhar" element={<RequireAuth><Compartilhar /></RequireAuth>} />
          <Route path="/favoritos" element={<RequireAuth><Favoritos /></RequireAuth>} />
          <Route path="/perfil" element={<RequireAuth><Perfil /></RequireAuth>} />
          <Route path="*" element={<Home />} />
        </Routes>
      </BrowserRouter>
    </SessaoProvider>
  )
}
