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
| Integrações | SMTP e ClamAV opcional |

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
- As variáveis `SMTP_*`, caso utilize os fluxos de envio de e-mail.
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

O ambiente descrito é local. O Compose atual não executa a aplicação completa;
uma publicação exige preparar os serviços do frontend/backend, persistência,
proxy reverso, rotas React, HTTPS e variáveis públicas dos fluxos de e-mail.
