# Banco Acadêmico

Plataforma colaborativa para compartilhar materiais de estudo, organizados por curso,
semestre e disciplina, com fórum acadêmico e moderação de publicações.

## Funcionalidades

- Cadastro, login, perfil e recuperação de senha.
- Envio, visualização e download de materiais acadêmicos.
- Busca e filtros por curso, semestre, disciplina e formato.
- Curtidas, descurtidas, favoritos, comentários e respostas.
- Fórum com discussões por disciplina.
- Administração da grade e aprovação ou rejeição de materiais.
- Interface com temas claro e escuro.

## Tecnologias

| Componente | Tecnologias |
| --- | --- |
| Frontend | React 18, Vite 8, Tailwind CSS 3 e React Router 7 |
| Backend | Java 21, Spring Boot e Spring Security com JWT |
| Dados | PostgreSQL e Flyway |
| Integrações | SMTP ou Resend, e ClamAV opcional |

O frontend está em `frontend/`; o backend segue a estrutura Maven em `src/`.
Consulte também o [README do frontend](frontend/README.md).

## Executar localmente

Pré-requisitos: Java 21, Node.js 22.13+ na linha 22 ou Node.js 24+, npm e PostgreSQL.
Para executar o banco pelo Compose, também é necessário Docker com Compose v2.

### 1. Configurar o ambiente

Crie um arquivo `.env` na raiz a partir de [`.env.example`](.env.example), se ele ainda
não existir. Preserve configurações já existentes e preencha:

- `POSTGRES_PASSWORD`: senha do banco local.
- `JWT_SECRET`: segredo aleatório forte com pelo menos 32 bytes.
- As variáveis `SMTP_*` ou as credenciais do Resend, conforme o provedor de e-mail.
- `MODERATION_ADMIN_EMAIL`, caso queira receber notificações de materiais pendentes.

O backend importa o `.env` quando iniciado na raiz. Use valores sem aspas nesse
arquivo; variáveis do ambiente do processo têm precedência. `DB_PASSWORD`, se definida,
substitui a senha usada pelo backend. Nunca versione credenciais.

### 2. Iniciar o PostgreSQL

Na raiz do projeto:

```bash
docker compose up -d postgres
```

A configuração local utiliza o banco `academicos`, usuário `postgres` e porta `5433`
do computador. O Compose mantém os dados em volume persistente.

### 3. Iniciar o backend

No Windows, a partir da raiz:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```bash
sh ./mvnw spring-boot:run
```

O backend utiliza `http://localhost:8080`. O Flyway aplica as migrations pendentes;
o Hibernate valida a estrutura. Para o primeiro uso, utilize um banco vazio dedicado
ao projeto. Faça backup antes de conectar uma instalação com dados existentes.

### 4. Iniciar o frontend

Em outro terminal:

```bash
cd frontend
npm ci
npm run dev
```

Acesse **http://localhost:5173/**. O Vite encaminha `/api` ao backend local.

## Verificações do frontend

Execute em `frontend/`:

```bash
npm test
npm run build
npm audit
```

Os testes de integração do backend devem utilizar banco e SMTP isolados, sem dados
ou destinatários reais. Iniciar apenas o frontend não inicia a API nem o banco.

## Dados e publicação

Os materiais ficam, por padrão, em `Uploads/`; as fotos de perfil ficam no PostgreSQL.
Preserve o banco e os arquivos e mantenha backups fora da máquina que os hospeda.
Arquivos locais, credenciais e backups não devem ser enviados ao Git.

## Executar a aplicação com Docker Compose

Use contêineres Linux no Docker Desktop ou em um host Linux. Na raiz do
repositório, configure `.env` a partir de `.env.example`. Preencha
`POSTGRES_PASSWORD` e um `JWT_SECRET` forte de pelo menos 32 bytes. Para um banco
já inicializado, use a senha atual do banco. O Compose lê os valores em tempo de
execução.

Em uma instalação nova, execute:

```sh
docker compose up -d --build --wait
docker compose ps
```

Abra **http://localhost:8081/**. O Nginx entrega o frontend e encaminha
`/api/` ao Spring. O backend usa `postgres:5432`; a porta `5433` do host fica
em loopback para o fluxo local sem Docker. Se mudar a porta ou o endereço pelo
qual acessa o frontend, atualize `DOCKER_PUBLIC_ORIGIN` para a origem exata.
`PROXY_SUBNET` e `PROXY_ADDRESS` podem ser alterados juntos se houver conflito
de rede. O backend confia apenas no IP configurado para o Nginx nessa rede.

Para usar o ClamAV, defina `UPLOAD_ANTIMALWARE_ENABLED=true` e execute
`docker compose --profile antimalware up -d --build --wait --wait-timeout 480`.
A primeira sincronização de assinaturas pode demorar. Verifique o estado do
scanner antes de testar uploads.

### Envio de e-mail

`EMAIL_PROVIDER=smtp` é o padrão e usa as variáveis `SMTP_*`. Para enviar
por HTTPS com o Resend, configure estas variáveis no ambiente privado do backend:

```dotenv
EMAIL_PROVIDER=resend
RESEND_API_KEY=
RESEND_FROM=onboarding@resend.dev
```

Crie uma chave com permissão de envio no painel do Resend e preencha
`RESEND_API_KEY` no `.env` ignorado pelo Git ou nos segredos do serviço.
Para enviar a usuários reais, verifique um domínio sob seu controle no Resend e
use um endereço desse domínio em `RESEND_FROM`. O remetente
`onboarding@resend.dev` é destinado a testes com o endereço da própria conta.
Ao usar Resend fora do Compose, configure `MANAGEMENT_HEALTH_MAIL_ENABLED=false`
para desativar a verificação de SMTP do Actuator.

Em Docker, defina `DOCKER_PUBLIC_ORIGIN` com a URL pública do frontend.
Fora do Compose, configure `EMAIL_CONFIRMATION_URL`, `PASSWORD_RESET_URL`
e `CORS_ALLOWED_ORIGINS` para o endereço público. A confirmação envia um
link. A recuperação envia um código para a tela de redefinição.

### Transição de uma instalação com dados

Antes de iniciar o backend em Docker com dados existentes, pare as gravações e
faça backup do PostgreSQL e dos materiais com
`bash scripts/backup.sh --aplicacao-parada DESTINO_NOVO PASTA_UPLOADS`. Se os
materiais já estiverem no volume Docker, use `--volume-docker` no último argumento.
Em Windows, execute os scripts com Bash e com o Docker acessível por esse shell.
Ensaie a restauração usando `scripts/restaurar-teste.sh` e um projeto novo.

O Compose mantém os nomes dos volumes de PostgreSQL e uploads. As fotos de
perfil ficam no banco. Os materiais da pasta local precisam ser importados
separadamente. Compare os arquivos com as referências do banco e interrompa
o procedimento em caso de colisão com conteúdo diferente. Para a primeira
importação, use um volume de destino vazio. Preserve a cópia original, valide
a correspondência dos hashes e atribua os arquivos importados ao UID/GID
`10001:10001` antes de iniciar o backend. Para referências absolutas antigas,
informe as raízes originais exatas em `UPLOAD_LEGACY_ROOTS`, separadas por ponto
e vírgula. Valide leitura, download e gravação com uma cópia isolada primeiro.

### Preparação para uma VPS

Na VPS, inclua o arquivo que remove as portas publicadas de PostgreSQL e ClamAV:

```sh
docker compose -f compose.yaml -f compose.vps.yaml config --quiet
docker compose -f compose.yaml -f compose.vps.yaml up -d --build --wait
```

Mantenha o frontend em loopback se o terminador HTTPS estiver no host.
Configure `DOCKER_PUBLIC_ORIGIN` com a URL pública, sem a barra final, e teste
SMTP, confirmação de e-mail e recuperação de senha antes de abrir o acesso.
Se houver um proxy TLS adicional, configure o Nginx para aceitar a indicação do
IP do cliente somente do endereço desse proxy. O proxy TLS deve sobrescrever o
cabeçalho de origem. Valide os limites por IP na topologia final.
