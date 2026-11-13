# Confirmação de e-mail

## Comportamento atual

O cadastro salva a conta sem enviar e-mail automaticamente. O login permite contas não confirmadas. Esta manutenção não tornou a confirmação obrigatória nem alterou JWT ou permissões.

O backend mantém os endpoints públicos `POST /auth/reenviar-confirmacao` e `POST /auth/confirmar-email`. O formulário atual de login não oferece botão de reenvio. A existência dos endpoints não significa que o cadastro esteja integrado a eles.

O reenvio aceita `{"email":"usuario@example.test"}` e responde genericamente para conta ausente, confirmada ou pendente. Para contas pendentes, gera 32 bytes aleatórios, representados em Base64URL (43 caracteres); somente SHA-256 é armazenado em `confirmacoes_email`. A validade é de 15 minutos, com uso único. O link usa o fragmento `#token=`; a confirmação recebe `{"token":"TOKEN_RECEBIDO"}`.

Uma substituição bem-sucedida invalida o link anterior. A substituição e o envio são executados em transação própria, com bloqueio da linha do usuário. Se Spring Mail lançar `MailException`, a transação é revertida antes do tratamento da falha: o link anterior mantém sua validade original e o candidato novo não fica ativo. Se não existia link, nenhum novo registro permanece. Isso não estende validade nem reativa links expirados.

A migration V8 existente preservou contas anteriores como confirmadas e definiu novas contas como pendentes. Não foi criada nem alterada migration nesta manutenção. A marca de confirmação não é exigida pelo login atual.

## Configuração e operação

O mesmo `JavaMailSender` atende confirmação, recuperação e moderação. Veja [RECUPERACAO_SENHA.md](RECUPERACAO_SENHA.md) e [EXECUCAO_LOCAL.md](EXECUCAO_LOCAL.md) para variáveis SMTP. O `application.properties` importa opcionalmente o `.env` da raiz como Java Properties; execute a partir da raiz e use valores sem aspas. Variáveis de ambiente prevalecem. Não versionar credenciais nem habilitar debug SMTP.

Conexão, leitura e escrita possuem timeouts de 5 segundos cada. Eles não representam um limite global de 5 segundos para toda a requisição. O envio é síncrono, sem fila ou retry automático.

Os logs informam fluxo e categoria controlada (`AUTENTICACAO`, `TIMEOUT`, `CONEXAO`, `PREPARACAO`, `FORMATO`, `TRANSPORTE` ou `INTERNA`), sem texto bruto da exceção, stack trace SMTP, destinatário ou token. Rejeições SMTP são classificadas como `TRANSPORTE`. A resposta genérica 200 não comprova envio ou entrega.

Após corrigir SMTP, é possível solicitar outro link respeitando os limites existentes: 10 solicitações por IP e 3 por e-mail por 15 minutos; confirmação limitada a 20 por IP e 5 por token. Atrás de proxy, configure identificação confiável de IP na infraestrutura.

## Limitação transacional

O SMTP participa antes do commit do banco, mas não é um recurso transacional. Se ele aceitar a mensagem e o commit falhar depois, o link enviado pode não ser utilizável. Uma falha de conexão também pode deixar a aceitação remota incerta. Não há garantia de entrega ou de envio exatamente uma vez. Retry e outbox exigem uma proposta separada; não foram adicionados.

## Testes seguros

`ConfirmacaoEmailTests` verifica hashes, expiração, uso único, substituição e rollback de falhas simuladas com JDBC/H2. `EmailFalhasTests` e os testes de reenvio verificam diagnóstico seguro. `SmtpLocalTests` usa `JavaMailSenderImpl` contra um servidor descartável em `127.0.0.1`, com portas efêmeras e dados fictícios. Não utiliza SMTP externo nem credenciais do `.env`. `SmtpRealTests` é opt-in e não faz parte desta verificação.

Regressão executada em 12/11/2026: 135 testes, 134 aprovados e 1 ignorado por falta de permissão para links simbólicos; zero falhas ou erros. Foram excluídos `ApplicationTests` (ambiente completo) e `SmtpRealTests` (envio externo).
