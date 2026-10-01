# Banco Acadêmico — Frontend

Interface em React 18, Vite 5 e Tailwind CSS 3, com React Router e ícones Lucide.
O visual usa azul-petróleo/verde-água, cores pastel para semestres, fontes Inter e
JetBrains Mono e alternância de tema claro/escuro.

## Executar localmente

Pré-requisitos: Node.js e npm, além da API Spring Boot com PostgreSQL disponível.
Na pasta do projeto:

```bash
cd frontend
npm ci
npm run dev
```

Acesse **http://localhost:5173**. O frontend está configurado para consumir o
backend em **http://localhost:8081**. Iniciar o Vite não inicia o backend nem o banco.
A porta 8081 é a porta publicada para o backend na configuração local usada nesta
sessão; o Spring Boot continua usando 8080 internamente por padrão.

Para permitir acesso ao servidor de desenvolvimento pela rede local:

```bash
npm run dev -- --host 0.0.0.0
```

Outros comandos:

```bash
npm run build    # gera os arquivos de produção em dist/
npm run preview  # serve a build para conferência local
```

O proxy da API está definido em `vite.config.js`, em `server.proxy`. Caso o
backend esteja em outra porta, altere `target` e reinicie o servidor Vite.
O frontend não lê o `.env` da raiz para descobrir a URL da API.

## Páginas e recursos

| Rota | Recursos |
| --- | --- |
| `/` | Apresentação pública da plataforma. |
| `/auth` | Login, cadastro e recuperação de senha simulados localmente. |
| `/painel` | Listagem de materiais, busca e filtros por curso, semestre, disciplina e tipo; favoritos, visualização, exclusão e atalhos para cadastro da grade. |
| `/admin` | Administração da grade: abas de cursos, semestres e disciplinas, contadores, busca, filtro por curso, cadastro, edição e exclusão com confirmação. |
| `/compartilhar` | Envio de arquivo com seleção de curso, semestre e disciplina, aviso de LGPD e confirmação de responsabilidade. |
| `/favoritos` | Materiais favoritados neste navegador. |
| `/perfil` | Nome e e-mail da sessão local. |
| `/sobre`, `/termos` | Informações públicas sobre a plataforma e seus termos. |

As rotas de painel, admin, compartilhamento, favoritos e perfil usam
`RequireAuth`: sem sessão local, redirecionam para `/auth`.
O acesso a `/admin` também aparece no cabeçalho e na navegação lateral.

## Como o frontend consome as APIs

As chamadas ficam centralizadas em `src/lib/api.js`, usando `fetch` e a base
relativa `/api`. Durante o desenvolvimento, o Vite remove esse prefixo e encaminha
a requisição ao Spring Boot:

```text
Navegador: GET http://localhost:5173/api/curso
Vite:     GET http://localhost:8081/curso
```

Assim, o navegador usa a mesma origem do frontend e não precisa de CORS para
esse fluxo local. Os dados acadêmicos são persistidos no backend; não há Supabase
nem TanStack Query/Router nesta implementação.

### Endpoints utilizados

Os caminhos abaixo são os do backend, sem o prefixo `/api` do frontend.

| Recurso | Listagem | Cadastro | Edição | Exclusão |
| --- | --- | --- | --- | --- |
| Cursos | `GET /curso` | `POST /curso` | `PUT /curso/{id}` | `DELETE /curso/{id}` |
| Semestres | `GET /semestres` | `POST /semestres` | `PUT /semestres/{id}` | `DELETE /semestres/{id}` |
| Disciplinas | `GET /disciplinas?size=500` | `POST /disciplinas` | `PUT /disciplinas/{id}` | `DELETE /disciplinas/{id}` |
| Materiais | `GET /material?size=500` | `POST /material` | Não utilizada na interface | `DELETE /material/{id}` |

Cursos e semestres retornam arrays. Disciplinas e materiais retornam objetos de
paginação do Spring Data; a interface lê `content`. Hoje ela solicita uma página
com até 500 registros e não percorre as demais páginas. Busca e filtros são
aplicados no navegador sobre os registros carregados.
Os semestres são ordenados no frontend por número e depois por nome.

Cadastros e edições da grade enviam JSON com `Content-Type: application/json`:

```json
{ "nome": "Ciência da Computação" }
```

```json
{ "numero": 1, "nome": "Primeiro semestre", "cursoId": "UUID_DO_CURSO" }
```

```json
{ "nome": "Algoritmos", "descricao": "Fundamentos de programação", "semestreId": "UUID_DO_SEMESTRE" }
```

Os IDs são UUIDs retornados pela API. A organização é
**Curso → Semestre → Disciplina → Material**. O número de semestre vai de 1 a 8.
Para começar com o banco vazio, cadastre um curso em `/admin`, depois seus
semestres e as disciplinas. Esses cadastros passam a aparecer no painel e no
formulário de envio. Não há cursos preenchidos automaticamente pelo frontend.
As alterações no admin recarregam suas listas; outras telas consultam a API ao
serem abertas. Registros com vínculos podem ter a exclusão recusada pelo backend.

Os filtros de curso e semestre do painel ficam nos parâmetros da URL, por exemplo
`/painel?curso=UUID_DO_CURSO&semestre=UUID_DO_SEMESTRE`.

### Upload de materiais

`POST /material` envia `FormData` com duas partes:

- `dados`: um `Blob` com tipo `application/json`, contendo `titulo`, `descricao` e `disciplinaId`.
- `arquivo`: o arquivo selecionado pelo usuário.

O frontend deixa o navegador definir o `Content-Type` multipart e seu boundary.
O limite de arquivo na interface é 20 MB; o backend também configura limites de
20 MB para arquivo e requisição multipart. Título e descrição são obrigatórios.
O envio bem-sucedido retorna ao painel no curso e semestre selecionados.

A API retorna metadados como `id`, `titulo`, `descricao`, `disciplinaId`,
`nomeArquivo`, `tipoArquivo`, `tamanhoArquivo` e `caminhoArquivo`.
`tipoArquivo` é usado para identificar PDF, imagem, slides ou outro formato.
`caminhoArquivo` é um caminho de armazenamento do servidor, não uma URL pública.

### Tratamento de respostas

O cliente trata `204` como resposta sem corpo e lê JSON nas demais respostas de
sucesso. Em falhas HTTP, tenta mostrar o campo `mensagem` da API; quando ele não
existe, usa `Erro <status>`. Falhas de conexão exibem uma mensagem para conferir
se o backend está rodando. As chamadas atuais não enviam token de autenticação.

## O que falta para aparecer a prévia dos arquivos

A interface já tem dois componentes de visualização:

- `PreviaArquivo.jsx`: prévia nos cards, com `iframe` para PDF e `img` para imagens.
- `VisualizadorMaterial.jsx`: modal com visualização maior e link para baixar.

Ambos usam a URL produzida por `api.urlArquivo(id)`:
**`/api/material/{id}/arquivo`**. Porém, o `MaterialController` atual ainda não
implementa **`GET /material/{id}/arquivo`**. Listar os metadados do material não
entrega o conteúdo do arquivo; por isso a prévia e o download ainda não funcionam.

Para completar essa integração, o backend precisa:

1. Implementar `GET /material/{id}/arquivo`, buscar o material pelo UUID e ler o
   arquivo associado a partir do armazenamento do servidor (atualmente `Uploads/`).
2. Retornar os bytes ou um recurso de arquivo, com `Content-Type` correspondente
   ao MIME salvo, como `application/pdf` ou `image/png`, e tamanho quando disponível.
3. Permitir a exibição embutida, usando `Content-Disposition: inline` com nome de
   arquivo adequado. O botão do frontend já usa o atributo `download`.
4. Retornar `404` para material ou arquivo inexistente e garantir que o arquivo
   servido pertença ao armazenamento permitido. Manter os uploads persistidos
   quando o backend executar em contêiner.
5. Garantir que os cabeçalhos de segurança permitam o `iframe` na origem do site.
   Quando houver autenticação real, definir também como `img`, `iframe` e links
   acessarão os arquivos protegidos, pois essas tags não usam o helper `fetch`.

Depois de implementar o endpoint, testar com um PDF e uma imagem reais enviados
pela interface: conferir a resposta do arquivo na aba de rede do navegador,
a prévia do card, o modal e o download.

**Slides, DOCX e outros formatos continuam sem prévia na implementação atual**:
a interface oferece o download e informa que o formato não tem visualização.
Para exibi-los no navegador será necessário um visualizador adicional ou uma
conversão para PDF/imagem. A exibição de PDF também depende do suporte do navegador.
Ainda falta um estado de erro/fallback quando o carregamento de uma prévia falha.

## Sessão local e permissões

O backend ainda não fornece autenticação de usuários; `SecurityConfig` permite
as requisições sem autenticação. `src/lib/sessao.jsx` guarda apenas nome e e-mail
em `localStorage`, sem validar credenciais no servidor ou persistir a senha.

O login verifica formato de e-mail, domínio contendo `unemat` e senha com pelo
menos seis caracteres. O cadastro exige também nome e validação local de força
da senha. A recuperação mostra uma confirmação simulada: nenhum e-mail é enviado.
Essas verificações não substituem a autenticação institucional no backend.

**Não existe papel de administrador implementado.** Qualquer usuário com sessão
local acessa `/admin`. A página administra a grade curricular; não inclui gestão
de usuários, atribuição de papéis ou aprovação/recusa de materiais.
Para restringi-la de verdade, faltam autenticação, papéis e autorização no backend,
além da verificação de permissões no frontend.

## Dados locais e funcionalidades pendentes

| Recurso | Situação atual / integração pendente |
| --- | --- |
| Cursos, semestres e disciplinas | CRUD integrado à API Spring Boot. |
| Materiais | Listagem, upload e exclusão integrados; falta servir o arquivo para prévia/download. |
| Login, cadastro, perfil e recuperação | Sessão e simulações locais; faltam endpoints reais e integração de autenticação. |
| Favoritos | IDs em `localStorage`, compartilhados pelas sessões do mesmo navegador; falta persistência por usuário no backend. |
| Tema | Preferência mantida no navegador. |
| Referência bibliográfica e responsabilidade | Validadas no formulário de envio; não enviadas nem persistidas pela API atual. |
| Administração | Gestão da grade implementada; faltam controle de papel, gestão de usuários e moderação. |
| Cores | Paleta por semestre; não há edição/persistência de cor por disciplina. |
| Estrelas e comentários | Ainda não implementados no frontend nem integrados à API. |
| Paginação | Listagem de materiais e disciplinas limitada à página solicitada; falta navegação ou carregamento das demais páginas. |

## Publicação

A build em `dist/` contém apenas arquivos estáticos. O proxy configurado em
`server.proxy` é do servidor de desenvolvimento, não acompanha esses arquivos.
No ambiente publicado, configure um proxy reverso para encaminhar `/api/*` ao
backend removendo `/api`, ou adapte a base em `src/lib/api.js` e configure CORS
se frontend e API tiverem origens distintas.

Configure também o servidor estático para devolver `index.html` nas rotas do
React Router, como `/admin` e `/painel`, para que links diretos e recargas funcionem.
Não coloque senhas do banco ou outros segredos nos arquivos do frontend.

## Estrutura

```text
src/
  App.jsx                   Rotas e provider de sessão
  lib/
    api.js                  Cliente HTTP e contratos com a API
    academico.js            Validações, cores e utilitários
    sessao.jsx              Sessão local
    useFavoritos.js         Favoritos locais
    useTema.js              Tema claro/escuro
  components/
    AppShell.jsx            Layout e navegação
    RequireAuth.jsx         Redirecionamento sem sessão local
    Gerenciar.jsx           Cadastros rápidos no painel
    MaterialCard.jsx        Card de material
    PreviaArquivo.jsx       Prévia no card
    VisualizadorMaterial.jsx Modal de visualização e download
    Modal.jsx               Modal compartilhado
    CampoSenha.jsx          Campo de senha e medidor
    ThemeToggle.jsx         Alternância de tema
  pages/
    Admin.jsx               Gestão de cursos, semestres e disciplinas
    Painel.jsx              Busca, filtros e materiais
    Compartilhar.jsx        Upload
    Auth.jsx, Perfil.jsx    Experiência de sessão local
    Favoritos.jsx           Materiais favoritados
    Home.jsx, Sobre.jsx, Termos.jsx
```
