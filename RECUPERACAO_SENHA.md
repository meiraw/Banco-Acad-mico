# Recuperação de senha por e-mail

## Fluxo atual

1. `POST /auth/esqueci-senha` recebe `{"email":"usuario@example.test"}` e responde genericamente: “Se houver uma conta para este e-mail, enviaremos um código de redefinição.”
2. Para conta existente, o e-mail HTML contém um código de quatro dígitos, válido por 15 minutos. O banco guarda somente SHA-256 de `"codigo:" + email + ":" + codigo`.
3. `POST /auth/validar-codigo-senha` recebe `{"email":"usuario@example.test","codigo":"1234"}`. Se válido, consome o código e retorna um token aleatório Base64URL de 43 caracteres. Somente seu hash permanece no banco, com a expiração original.
4. `POST /auth/redefinir-senha` recebe `{"token":"TOKEN_RETORNADO_NA_VALIDACAO","novaSenha":"NovaSenha123!"}`. Salva BCrypt, incrementa `versao_credenciais` e remove recuperações da conta. Tokens não são enviados em links nesta modalidade.

Senha: mínimo de 8 caracteres, não composta apenas por espaços, máximo de 72 bytes UTF-8. JWTs anteriores são invalidados pela versão de credenciais existente. Esta manutenção não alterou autenticação, JWT, permissões ou regras de senha.

As rotas são públicas e devem ser chamadas sem Authorization. Com proxy Vite, o frontend usa `/api/auth/...`. O e-mail é normalizado com trim e minúsculas; este endpoint não exige domínio institucional. O React mantém o código e o token apenas durante o fluxo de recuperação.

## Falhas e consistência

A troca dos registros e o envio ocorrem em transação própria. `MailException` sai da transação e provoca rollback antes de ser tratada. Assim, falha no reenvio preserva o código ou token anterior e sua expiração original. Na primeira solicitação com falha, nenhum candidato fica armazenado. Um envio bem-sucedido substitui a recuperação anterior. O bloqueio de usuário serializa essas operações.

A resposta pública permanece genérica em falha de envio; não garante entrega nem tempo de resposta constante. Os logs incluem apenas fluxo e categoria controlada, sem destinatário, código, token, credenciais ou mensagem bruta do SMTP. Rejeições SMTP ficam na categoria `TRANSPORTE`. Erros de banco não são silenciosamente tratados como falha SMTP.

Não há retry automático, fila ou garantia de entrega. O usuário pode solicitar novo código dentro dos limites. SMTP aceito seguido de falha no commit ainda pode produzir um código inutilizável; timeout após aceitação também pode deixar o resultado remoto incerto. Evitar duplicação garantida exigiria outro desenho, não implementado nesta manutenção.

## Configuração

O Spring Mail já era dependência do projeto. Nenhuma biblioteca ou migration foi acrescentada nesta manutenção.

| Variável | Padrão / finalidade |
| --- | --- |
| SMTP_HOST | localhost |
| SMTP_PORT | 587 |
| SMTP_USERNAME / SMTP_PASSWORD | Credenciais; padrão vazio |
| SMTP_FROM | nao-responda@localhost; configurar remetente autorizado |
| SMTP_AUTH | true |
| SMTP_STARTTLS | true; habilita e exige STARTTLS |
| PASSWORD_RESET_URL | http://localhost:5173/redefinir-senha; configuração mantida, não é link enviado no fluxo por código |
| EMAIL_CONFIRMATION_URL | http://localhost:5173/confirmar-email |
| MODERATION_ADMIN_EMAIL | Destinatário da moderação; vazio desabilita a notificação |

O `.env` é importado opcionalmente como Java Properties ao iniciar da raiz; valores sem aspas. Variáveis de ambiente têm prioridade. Não versionar segredos nem habilitar debug SMTP. Timeouts de conexão, leitura e escrita: 5000 ms cada, sem limite global de cinco segundos. Para SMTP de captura local sem TLS/autenticação, configure explicitamente `SMTP_AUTH=false` e `SMTP_STARTTLS=false` apenas nesse ambiente.

## Limites existentes

Janelas de 15 minutos, contadores persistidos no PostgreSQL: solicitação 10/IP e 3/e-mail; validação de código 20/IP e 5/e-mail; redefinição 20/IP e 5/token. Limite por endereço na solicitação retorna resposta genérica; os demais bloqueios retornam 429. Código/token inválido, expirado ou consumido retorna 400. Chaves de limitação são hashes. Configuração confiável de IP atrás de proxy é responsabilidade da infraestrutura.

## Moderação

`ModeracaoEmailService` envia sincronamente depois do commit do material pendente. Falha é registrada por categoria e não desfaz o material. Consultas não repetem o envio. Sem destinatário configurado, registra aviso e não envia. Não há recuperação automática das notificações perdidas; a fila de moderação continua disponível. Essa regra foi preservada.

## Verificação segura

As suítes `ConfirmacaoEmailTests` e `RecuperacaoSenhaTests` usam JDBC/H2 para verificar rollback e preservação de registros anteriores. `ModeracaoTests` verifica material íntegro nas falhas e ausência de envio em rollback. Os testes verificam categorias e ausência de segredos nos logs. `SmtpLocalTests` cobre aceitação local, conexão recusada, credenciais rejeitadas, destinatário rejeitado, interrupção e timeout de leitura com servidor descartável em loopback. Não comprova entrega externa, TLS de produção, timeout real de conexão/escrita ou comportamento do provedor SMTP.

Regressão executada com Maven 3.9.16 e Java 21:

```text
mvn -Dtest=*Tests,!ApplicationTests,!SmtpRealTests -Dsmtp.real=false -Dspring.profiles.active=test test
```

Resultado: `BUILD SUCCESS`; 135 testes, 134 aprovados, 1 ignorado por falta de permissão para criar links simbólicos, zero falhas/erros. O wrapper local não iniciou; foi usado o Maven já instalado. Não executar `SmtpRealTests` sem autorização explícita para envio externo. `ApplicationTests` depende do ambiente completo e não integrou esta verificação.
