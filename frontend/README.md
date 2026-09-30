# Banco Acadêmico — Frontend

React + Vite + Tailwind, com o design do protótipo (paleta azul-petróleo/verde-água, Inter + JetBrains Mono).
Consome a API Spring Boot real em `http://localhost:8080` via proxy `/api` (sem CORS).

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
```

## Páginas

- **/** — landing pública
- **/auth** — login e cadastro
- **/painel** — busca, filtros por semestre/disciplina/tipo, grade de materiais
- **/compartilhar** — upload de material (com aviso de LGPD e checkbox de responsabilidade)
- **/favoritos**, **/perfil**, **/sobre**, **/termos**

## ⚠️ Login local — leia antes de usar

O backend Spring Boot **ainda não tem cadastro/login** (`SecurityConfig` libera tudo, sem JWT).
Para não travar a experiência do protótipo, `/auth` guarda a "sessão" só no navegador
(`localStorage`), sem chamar o servidor. Qualquer nome/e-mail `@unemat.br` entra.
Quando as US03/US04 (cadastro real + JWT) existirem no backend, troque `src/lib/sessao.jsx`
para chamar a API de verdade — o resto da interface (rotas protegidas, cabeçalho, perfil)
já está pronto para isso.

## Ajustes necessários no backend (para tudo funcionar)

1. **Expor `semestreId` da disciplina.** `DisciplinasResponseDTO` guarda o campo mas não
   tem o getter — sem ele o Jackson não serializa, e o front não sabe filtrar por semestre:
   ```java
   public UUID getSemestreId() { return semestreId; }
   ```
2. **Endpoint de download/prévia.** Crie `GET /material/{id}/arquivo` em `MaterialController`
   devolvendo o arquivo salvo em `Uploads/` (com `Content-Type` do `tipoArquivo`).

## O que é só front-end (sem endpoint no backend ainda)

- **Login/cadastro** — local ao navegador, ver aviso acima.
- **Favoritos** — guardados em `localStorage`, por navegador (sem endpoint de favoritos).
- **Avaliação por estrelas e comentários (US08, US09)** — não implementados; o backend
  não tem tabelas de avaliação/comentário ainda.
- **Painel administrativo (US11)** — não implementado; sem endpoint de moderação.

## Estrutura

```
src/
  lib/        api.js, academico.js (regras), sessao.jsx, useFavoritos.js, useTema.js
  components/ AppShell, MaterialCard, VisualizadorMaterial, PreviaArquivo, CampoSenha, Modal, Gerenciar
  pages/      Home, Auth, Painel, Compartilhar, Favoritos, Perfil, Sobre, Termos
```
