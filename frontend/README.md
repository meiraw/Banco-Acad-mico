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
| `/auth` | Login, cadastro e pedido de recuperação de senha, integrados à API (JWT). |
| `/redefinir-senha` | Definição da nova senha a partir do link enviado por e-mail (token no fragmento `#token=...`). |
| `/confirmar-email` | Confirmação de e-mail a partir do link recebido (fluxo disponível, mas não exigido hoje no cadastro/login). |
| `/painel` | Listagem de materiais, busca e filtros por curso, semestre, disciplina e tipo; favoritos, visualização, exclusão e atalhos para cadastro da grade. |
| `/admin` | Somente `ADMIN`. Administração da grade: abas de cursos, semestres e disciplinas, contadores, busca, filtro por curso, cadastro, edição e exclusão com confirmação. |
| `/compartilhar` | Envio de arquivo com seleção de curso, semestre e disciplina, aviso de LGPD e confirmação de responsabilidade. |
| `/favoritos` | Materiais favoritados neste navegador. |
| `/perfil` | Nome e e-mail da sessão atual. |
| `/sobre`, `/termos` | Informações públicas sobre a plataforma e seus termos. |

As rotas de painel, admin, compartilhamento, favoritos e perfil usam
`RequireAuth`: sem sessão válida, redirecionam para `/auth`. Com a propriedade
`admin`, a rota `/admin` também redireciona para `/painel` quem não tem perfil `ADMIN`.
Os atalhos para `/admin` (cabeçalho, navegação lateral e painel) só aparecem para `ADMIN`.

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
| Disciplinas | `GET /disciplinas?page=N&size=200` | `POST /disciplinas` | `PUT /disciplinas/{id}` | `DELETE /disciplinas/{id}` |
| Materiais | `GET /material?page=N&size=200` | `POST /material` | Não utilizada na interface | `DELETE /material/{id}` |

Cursos e semestres retornam arrays. Disciplinas e materiais retornam objetos de
paginação do Spring Data. O helper `lista()` de `src/lib/api.js` pede páginas de
200 registros, uma após a outra, e junta o `content` de todas elas até chegar à
última página (usa `totalPages`; se o campo não vier, para quando uma página
chega incompleta). Há uma trava de 50 páginas (10.000 registros) contra laços
infinitos. Busca e filtros são aplicados no navegador sobre os registros
carregados, então continuam enxergando todos os dados. Como a listagem não envia
`sort`, a ordem entre páginas depende do banco; se o volume crescer muito, vale
definir uma ordenação estável no backend.
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
existe, usa `Erro <status>`. O erro lançado carrega `status`, usado, por exemplo,
para tratar `429` (muitas tentativas) e `400` (link inválido). Falhas de conexão
exibem uma mensagem para conferir se o backend está rodando.

As chamadas enviam `Authorization: Bearer <token>` automaticamente, lendo o JWT do
`sessionStorage`. As rotas marcadas como `publico` (cadastro, login, confirmação de
e-mail e recuperação de senha) não enviam o cabeçalho, pois o backend rejeita
qualquer JWT inválido ou antigo mesmo nessas rotas. Uma resposta `401` indica
token ausente, inválido, expirado ou invalidado por troca de senha.

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
   Hoje o `GET /material/**` é público no backend, então `img`, `iframe` e links
   funcionam sem token. Se essa rota for protegida no futuro, será preciso definir
   como essas tags acessarão os arquivos, pois elas não usam o helper `fetch`
   (e, portanto, não enviam o cabeçalho `Authorization`).

Depois de implementar o endpoint, testar com um PDF e uma imagem reais enviados
pela interface: conferir a resposta do arquivo na aba de rede do navegador,
a prévia do card, o modal e o download.

**Slides, DOCX e outros formatos continuam sem prévia na implementação atual**:
a interface oferece o download e informa que o formato não tem visualização.
Para exibi-los no navegador será necessário um visualizador adicional ou uma
conversão para PDF/imagem. A exibição de PDF também depende do suporte do navegador.
Ainda falta um estado de erro/fallback quando o carregamento de uma prévia falha.

## Autenticação e permissões

A autenticação é real e feita pelo backend (Spring Security + JWT). O fluxo:

1. `Auth.jsx` chama `POST /usuario` (cadastro) e `POST /auth/login`; o login devolve `{ token }`.
2. `src/lib/sessao.jsx` guarda o JWT e os dados da sessão (nome e e-mail) em
   `sessionStorage`, sob as chaves `banco-academico:token` e `banco-academico:sessao`.
   A sessão some ao fechar a aba, é removida no logout e expira sozinha quando o
   token vence (2 horas). A senha nunca é guardada.
3. O perfil (`USER` ou `ADMIN`) é lido do claim `perfil` do JWT apenas para
   decidir o que mostrar na interface. **A autorização efetiva é sempre do
   backend**, que consulta o perfil atual no banco a cada requisição.

Regras de formulário: o e-mail pode ser de qualquer provedor (a validação é só de
formato) e a senha precisa de pelo menos 8 caracteres, tanto no login quanto no
cadastro, igual ao mínimo do backend. O cadastro exige ainda nome com 3+ caracteres e
força de senha mínima ("Média"). O login não pede confirmação de e-mail.
O login inválido retorna `409` com mensagem genérica (contrato preservado no backend).

Recuperação de senha: a aba "Recuperar acesso" chama `POST /auth/esqueci-senha` e
mostra sempre a mesma mensagem, exista a conta ou não. O e-mail traz um link para
`/redefinir-senha#token=...`; a tela mantém o token só em memória, remove o
fragmento da barra de endereços e chama `POST /auth/redefinir-senha`. O link vale
15 minutos e só pode ser usado uma vez. Depois da troca, todas as sessões antigas
da conta recebem `401`. Para o e-mail ser entregue, o backend precisa de SMTP
configurado (veja `RECUPERACAO_SENHA.md`).

Permissões no backend (resumo; detalhes em `RELATORIO_AUTENTICACAO.md`):

| Operação | Quem pode |
| --- | --- |
| Cadastro, login, confirmação e recuperação | Público |
| Consultar cursos, semestres, disciplinas e materiais (`GET`) | Público |
| Enviar material (`POST /material`) | `USER` ou `ADMIN` |
| Criar, editar e excluir cursos, semestres e disciplinas | `ADMIN` |
| Editar e excluir materiais | `ADMIN` |
| Gestão de usuários (`/usuario`, exceto cadastro) | `ADMIN` |

O papel `ADMIN` existe, mas **não há tela nem endpoint público para promover
alguém**: o primeiro administrador precisa ser provisionado por um procedimento
confiável (por exemplo, direto no banco). A tela `/admin` administra a grade
curricular; não inclui gestão de usuários, atribuição de papéis ou
aprovação/recusa de materiais. Não há revogação individual de JWT no logout: o
token deixa de existir no navegador, mas continua válido até expirar.

## Dados locais e funcionalidades pendentes

| Recurso | Situação atual / integração pendente |
| --- | --- |
| Cursos, semestres e disciplinas | CRUD integrado à API Spring Boot. |
| Materiais | Listagem, upload e exclusão integrados; falta servir o arquivo para prévia/download. |
| Login, cadastro e sessão | Integrados à API com JWT em `sessionStorage`. |
| Recuperação de senha | Integrada à API; depende de SMTP configurado no backend. |
| Confirmação de e-mail | Tela e API existem, mas o cadastro e o login não exigem a confirmação. |
| Perfil | Mostra nome e e-mail da sessão (o nome vem do cadastro ou, em logins posteriores, da parte do e-mail antes do `@`, pois o login só devolve o token). |
| Favoritos | IDs em `localStorage`, compartilhados pelas sessões do mesmo navegador; falta persistência por usuário no backend. |
| Tema | Preferência mantida no navegador. |
| Referência bibliográfica e responsabilidade | Validadas no formulário de envio; não enviadas nem persistidas pela API atual. |
| Administração | Gestão da grade restrita a `ADMIN`; faltam gestão de usuários, promoção de papéis e moderação. |
| Cores | Paleta por semestre; não há edição/persistência de cor por disciplina. |
| Estrelas e comentários | Ainda não implementados no frontend nem integrados à API. |
| Paginação | Listagens de materiais e disciplinas carregam todas as páginas (200 por requisição); busca e filtros seguem no navegador. Não há paginação visual na interface. |

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
    sessao.jsx              Sessão (JWT e dados em sessionStorage)
    useFavoritos.js         Favoritos locais
    useTema.js              Tema claro/escuro
  components/
    AppShell.jsx            Layout e navegação
    RequireAuth.jsx         Redirecionamento sem sessão (e sem perfil ADMIN em /admin)
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
    Auth.jsx, Perfil.jsx    Login, cadastro, recuperação e perfil
    ConfirmarEmail.jsx      Confirmação de e-mail pelo link
    RedefinirSenha.jsx      Nova senha a partir do link do e-mail
    Favoritos.jsx           Materiais favoritados
    Home.jsx, Sobre.jsx, Termos.jsx
```
