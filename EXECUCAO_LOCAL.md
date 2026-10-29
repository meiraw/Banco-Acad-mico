# Execução local integrada

Execute o backend a partir da raiz do projeto (no IntelliJ, configure esse diretório como Working directory).
O Spring importa automaticamente o `.env` da raiz como Java Properties. Use `CHAVE=valor`, sem aspas.
Variáveis do ambiente do processo têm prioridade sobre o arquivo.
Não coloque credenciais em arquivos do frontend nem use prefixo VITE_ para segredos.

## Configuração

Use `.env.example` como referência e complete o `.env` existente sem perder a senha do banco.
`POSTGRES_PASSWORD` configura o PostgreSQL do Docker e também é usada pelo backend quando `DB_PASSWORD` não está definido.
Preencha `JWT_SECRET` com um segredo aleatório de pelo menos 32 caracteres.

Para recuperação de senha real, preencha SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD e SMTP_FROM com dados autorizados do provedor.
O exemplo usa SMTP com STARTTLS na porta 587. No Gmail, a credencial é uma senha de aplicativo, conforme a disponibilidade na conta.
Não é necessário ativar o perfil gmail se essas variáveis estiverem preenchidas.
Não envie credenciais por chat nem versione o `.env`.

## Inicialização

1. Na raiz: `docker compose up -d postgres`.
2. Na raiz: `./mvnw.cmd spring-boot:run` ou execute Application no IntelliJ com Java 21.
3. Em outro terminal, na pasta frontend: `npm run dev`.
4. Abra http://localhost:5173.

O frontend usa 5173 e encaminha `/api/*` ao backend em 8080, removendo `/api`.
O Vite recusa iniciar em outra porta se 5173 estiver ocupada, preservando os links de e-mail.
Os servidores precisam ser iniciados separadamente. Reinicie o backend após alterar o `.env`.
O proxy Vite vale para desenvolvimento; a hospedagem de produção precisa configurar seu próprio proxy e fallback de rotas React.

## Conferir a recuperação

Cadastre uma conta com um e-mail de teste que você controla. Em Esqueci minha senha, solicite o link,
confira a caixa de entrada e spam e abra o link em até 15 minutos. Defina a nova senha e confira o login.
A resposta 200 não comprova entrega: também é usada para conta ausente, limite por endereço ou falha SMTP.
Em falha SMTP, o backend registra uma advertência e remove o token. Não há envio de aviso após a troca da senha.

Para usuários fora desta máquina, altere PASSWORD_RESET_URL e EMAIL_CONFIRMATION_URL para as URLs públicas HTTPS do frontend.
Um link localhost só abre o projeto na própria máquina de quem clica.
