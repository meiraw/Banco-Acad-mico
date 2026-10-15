# Confirmação de e-mail

> Configuração atual: cadastro e login aceitam endereços de qualquer provedor e não exigem confirmação por e-mail nem envio SMTP. O cadastro entra diretamente após criar a conta. A validação de formato não comprova a existência do endereço. Os componentes de confirmação descritos abaixo permanecem disponíveis, mas não são acionados pelo cadastro nem bloqueiam o acesso. SMTP só é necessário para enviar mensagens, como na recuperação de senha. Não é necessário ativar o perfil gmail para cadastro e login; se foi ativado apenas para isso, remova gmail de SPRING_PROFILES_ACTIVE.

Novos cadastros precisam confirmar a posse do e-mail antes do login. A senha é exclusiva do Banco Acadêmico; nunca se pede a senha da caixa de entrada do usuário. A restrição institucional da interface continua existente.

## Configuração

No IntelliJ, em Run > Edit Configurations > Environment variables, mantenha DB_PASSWORD e JWT_SECRET e acrescente as configurações do seu provedor:

```text
SMTP_HOST=servidor-do-provedor
SMTP_PORT=587
SMTP_USERNAME=usuario-de-envio
SMTP_PASSWORD=credencial-de-envio
SMTP_FROM=remetente-autorizado
SMTP_AUTH=true
SMTP_STARTTLS=true
EMAIL_CONFIRMATION_URL=https://seu-frontend/confirmar-email
PASSWORD_RESET_URL=https://seu-frontend/redefinir-senha
```

Os valores são ilustrativos. Use um servidor com STARTTLS na porta configurada. A credencial SMTP pode ser uma senha de aplicativo ou credencial específica do serviço, conforme o provedor. Não coloque segredos no Git. O Spring não carrega `.env` automaticamente. Reinicie o backend após alterar o ambiente.

As duas URLs são do **frontend** e precisam estar acessíveis para quem recebe o e-mail. Para testes no mesmo computador podem ser `http://localhost:5173/confirmar-email` e `http://localhost:5173/redefinir-senha`. Para testes em redes diferentes, use a URL pública do frontend e configure seu proxy para alcançar a API. O túnel apenas do backend não publica o frontend. Configure CORS_ALLOWED_ORIGINS quando houver chamadas diretas entre origens e fallback do React nas duas rotas.

## Envio pelo Gmail

Foi criado `src/main/resources/application-gmail.properties`, com SMTP do Gmail na porta 587, autenticação e STARTTLS obrigatório. Ative esse perfil para usar Gmail sem alterar as configurações dos demais ambientes.

1. Na conta que enviará as mensagens, habilite a verificação em duas etapas.
2. Acesse https://myaccount.google.com/apppasswords e gere uma senha de aplicativo para “Banco Acadêmico”. Não use a senha comum da conta. A disponibilidade depende das políticas da conta; em contas institucionais Google Workspace pode ser necessário falar com o administrador.
3. No IntelliJ, abra Run > Edit Configurations, selecione o backend e abra a tabela de Environment variables. Mantenha DB_PASSWORD e JWT_SECRET e adicione:

| Nome | Valor |
| --- | --- |
| SPRING_PROFILES_ACTIVE | gmail |
| SMTP_USERNAME | endereço Gmail que enviará as mensagens |
| SMTP_PASSWORD | senha de aplicativo gerada, sem os espaços de apresentação |
| EMAIL_CONFIRMATION_URL | http://localhost:5173/confirmar-email para teste local |
| PASSWORD_RESET_URL | http://localhost:5173/redefinir-senha para teste local |

Se já houver outros perfis ativos, acrescente `gmail` à lista separada por vírgulas. Se SMTP_FROM já estiver definido, use o mesmo endereço de SMTP_USERNAME, a menos que tenha um remetente alternativo autorizado. Sem SMTP_FROM, o perfil usa automaticamente SMTP_USERNAME.

4. Reinicie o backend e o frontend. Na tela de login use “Reenviar confirmação de e-mail” para uma conta pendente ou solicite recuperação de uma conta cadastrada. Confira também spam e o console do backend. Não compartilhe links com tokens.
5. Abra o link recebido, confirme a conta e faça login. Para links abertos em outro computador, substitua localhost pela URL acessível do frontend antes de solicitar o envio.

O remetente pode ser Gmail e o destinatário institucional; o provedor do destinatário não muda as configurações de envio. O frontend continua exigindo e-mail UNEMAT no cadastro. Para cadastrar Gmail como usuário seria preciso alterar essa regra separadamente.

Falhas comuns: credenciais recusadas indicam senha de aplicativo ou política da conta; timeout indica conectividade com o SMTP; remetente recusado exige revisar SMTP_FROM. Uma resposta 200 na API não garante entrega. Não habilite logs SMTP contendo dados de autenticação.

Referências: https://support.google.com/mail/answer/185833 e https://support.google.com/mail/answer/7104828.

## Fluxo de confirmação

- `POST /usuario` cria uma conta pendente, salva a senha com BCrypt e tenta enviar o link. O frontend mostra orientação para verificar a caixa de entrada, sem login automático.
- O link contém `#token=...`; a tela mantém o token em memória e remove o fragmento da barra. Só o clique em “Confirmar meu e-mail” consome o token.
- `POST /auth/confirmar-email`, corpo `{"token":"TOKEN_DO_EMAIL"}`, ativa a conta. Link inválido, expirado ou reutilizado retorna 400.
- `POST /auth/reenviar-confirmacao`, corpo `{"email":"usuario@unemat.br"}`, retorna mensagem genérica para conta ausente, já confirmada ou pendente. O botão está no login.
- Login de conta pendente com senha correta retorna 403. JWTs de conta pendente são rejeitados com 401.
- Uma troca de e-mail pela administração exige nova confirmação, revoga recuperações de senha pendentes e incrementa a versão das credenciais para invalidar sessões anteriores.

Essas chamadas públicas não enviam Authorization. Com o proxy Vite, o frontend usa `/api/auth/...`; diretamente na API, `/auth/...`.

## Dados e limites

A migration V8 cria `confirmacoes_email` e `usuarios.email_confirmado`. Contas anteriores à migration permanecem confirmadas para preservar o acesso; isso é compatibilidade e **não prova que seus endereços foram verificados**. Novas contas começam pendentes.

Tokens aleatórios de 32 bytes são guardados somente como SHA-256, valem por 15 minutos e só podem ser usados uma vez. O reenvio invalida o link anterior. Confirmação e reenvio bloqueiam a linha do usuário para serializar operações concorrentes.

Reenvio: até 10 por IP e 3 por e-mail em janela de 15 minutos. Confirmação: até 20 por IP e 5 por token. Atrás de proxy, a infraestrutura precisa resolver o IP de forma confiável; sem isso clientes podem compartilhar a cota do proxy.

Uma falha SMTP mantém a conta pendente e remove o token não enviado. O log registra mensagem fixa, sem token ou destinatário. A resposta de reenvio continua genérica: status 200 não comprova entrega. Depois de corrigir SMTP, use o reenvio.

## Verificação

```powershell
.\mvnw.cmd '-Dtest=AuthenticationTests,RecuperacaoSenhaTests,RecuperacaoEndpointTests,ConfirmacaoEmailTests' test
cd frontend
npm run build
```

Os testes automatizados simulam SMTP. A entrega real e a aplicação da migration no PostgreSQL devem ser verificadas no ambiente de implantação. Para o teste manual, cadastre uma conta nova, abra o e-mail, confirme, faça login e teste recuperação de senha. Não reabra um token já consumido como teste de sucesso.
