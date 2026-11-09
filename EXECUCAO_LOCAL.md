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

## Moderação e notificações

Configure `MODERATION_ADMIN_EMAIL` no `.env` com o destinatário administrativo autorizado.
As notificações reutilizam `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` e `SMTP_FROM`.
Reinicie o backend após a configuração. Sem destinatário, os materiais são salvos e um aviso é registrado no log.

Novos uploads começam como `PENDENTE`. Em **Perfil → Administração → Moderação de materiais**, administradores
podem visualizar/baixar o arquivo, aprovar ou rejeitar com justificativa. O autor acompanha o status em
**Minhas publicações**. Somente aprovados aparecem na listagem geral; autor e administradores podem acessar
materiais pendentes e rejeitados com JWT. As decisões repetidas retornam HTTP 409.

A migration `V14__moderacao_materiais.sql` preserva a visibilidade dos registros existentes, marcando-os como
`APROVADO`, sem inventar data ou administrador de aprovação. Apenas novos registros recebem `PENDENTE` por padrão.
Nenhum arquivo rejeitado é apagado automaticamente.

O e-mail é tentado uma vez por upload confirmado, após o commit, sem anexos ou JWT. Consultas e decisões não
reenviam notificações. Falhas SMTP são registradas sem credenciais e não revertem o material. O envio usa os
timeouts SMTP existentes e não possui fila persistente nem retentativa automática: em falhas ou encerramento
do processo após o commit, consulte a fila no painel. Um novo envio do formulário é um novo material.

Endpoints administrativos (JWT de ADMIN): `GET /admin/materiais/pendentes?pagina=0`,
`PATCH /admin/materiais/{id}/aprovar` e `PATCH /admin/materiais/{id}/rejeitar`
(JSON: `{"justificativa":"Motivo da rejeição"}`). A visualização usa o endpoint existente
`GET /material/{id}/arquivo`, enviando Bearer para materiais restritos.

## Fotos e exclusão de publicações

A foto de perfil é substituída no mesmo registro de `fotos_perfil`: apenas a imagem atual é mantida.
Uma imagem inválida não substitui a foto existente. Não há cópias de fotos na pasta `Uploads`.

Em **Perfil → Minhas publicações**, o autor pode excluir seus materiais após confirmar a ação.
Administradores também podem excluir materiais. A publicação, seus comentários, respostas e curtidas
são removidos em transação. O arquivo é apagado após o commit, com caminho restrito à pasta `Uploads`.
A migration `V17__fila_exclusao_arquivos.sql` cria uma fila persistente para repetir falhas de remoção
a cada minuto, inclusive após reiniciar o backend. Em caso de rollback, o arquivo é preservado.

## Curtidas e dislikes

Na publicação, **Curtir** e **Descurtir (👎)** têm contagens próprias. Cada conta pode manter somente
uma dessas reações por material; escolher a outra substitui a anterior. Clicar novamente remove a reação.
A migration `V18__reacoes_negativas.sql` preserva as curtidas existentes. Os favoritos continuam separados.
As reações exigem login e respeitam o acesso aos materiais moderados.

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

Em Esqueci minha senha, solicite o código enviado por e-mail e informe os 4 dígitos na tela de recuperação em até 15 minutos. A validação abre a página de nova senha. O código tem uso único e limite de 5 tentativas por e-mail a cada janela de 15 minutos. A autorização fica apenas em memória: atualizar a página exige outro código.

Para usuários fora desta máquina, altere PASSWORD_RESET_URL e EMAIL_CONFIRMATION_URL para as URLs públicas HTTPS do frontend.
Um link localhost só abre o projeto na própria máquina de quem clica.
