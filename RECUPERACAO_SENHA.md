# Recuperação de senha por e-mail

## Implementação

Inventário anterior à alteração: Spring Mail já constava no pom.xml. Não existiam serviço de envio, configuração SMTP, DTOs, armazenamento ou endpoints de recuperação. Reutilizados Spring Mail, PasswordEncoder BCrypt, tratamento de erros, autenticação e autorização existentes. Frontend preservado.

A migration nova V7 acrescenta versao_credenciais ao usuário, recuperacoes_senha e limites_recuperacao. Migrations anteriores não foram alteradas. Flyway aplica V7 ao iniciar; faça o backup habitual antes de atualizar produção.

Token: 32 bytes de SecureRandom, Base64 URL sem padding (43 caracteres), armazenado apenas como SHA-256, validade de 15 minutos. Cada solicitação efetiva substitui as anteriores. A redefinição bloqueia a linha do usuário, revalida o token após o bloqueio, salva BCrypt, incrementa a versão e remove todas as recuperações da conta na mesma transação. A exclusão de usuário também remove seus tokens.

JWT continua com validade de duas horas e mesmas permissões. Novos JWTs incluem claim vc. O filtro compara a versão com o banco já consultado a cada requisição. JWTs anteriores sem vc equivalem a zero; após redefinição todos os JWTs antigos da conta recebem 401. Novo login emite token com a versão atual. Não há tabela de JWTs nem uso de JWT como token de recuperação.

## Ambiente

Variáveis existentes DB_PASSWORD, JWT_SECRET e CORS_ALLOWED_ORIGINS continuam necessárias conforme implantação.

| Variável | Uso / padrão local |
| --- | --- |
| SMTP_HOST | servidor SMTP; padrão localhost |
| SMTP_PORT | porta; padrão 587 |
| SMTP_USERNAME | usuário SMTP; padrão vazio |
| SMTP_PASSWORD | segredo SMTP; padrão vazio |
| SMTP_FROM | remetente autorizado; padrão nao-responda@localhost |
| SMTP_AUTH | autenticação; padrão true |
| SMTP_STARTTLS | habilita e exige STARTTLS; padrão true |
| PASSWORD_RESET_URL | URL absoluta da tela React, sem query ou fragmento; padrão http://localhost:5173/redefinir-senha |

Em produção configure remetente válido e URL HTTPS. Para servidor de captura local sem autenticação/TLS, use SMTP_AUTH=false e SMTP_STARTTLS=false. Timeouts SMTP de conexão/leitura/escrita: 5 segundos. O backend importa o .env da raiz como Java Properties ao executar a partir da raiz; use valores sem aspas. Variáveis no ambiente do processo têm prioridade. Consulte EXECUCAO_LOCAL.md. Não versionar credenciais nem habilitar debug SMTP ou logs de corpos de requisições nessas rotas.

## Contratos JSON

Rotas públicas, Content-Type: application/json. Não enviar Authorization: um JWT antigo/inválido presente no header continua sendo rejeitado pelo filtro de segurança existente. No proxy Vite atual, o React chama /api/auth/...; diretamente no backend, /auth/....

### POST /auth/esqueci-senha

Requisição:

```json
{"email":"nome@unemat.br"}
```

200 para endereço cadastrado, não cadastrado, suprimido pelo limite por endereço ou falha de envio SMTP:

```json
{"mensagem":"Se houver uma conta para este e-mail, enviaremos um link de redefinição."}
```

E-mail obrigatório, formato válido, até 200 caracteres; normalizado com trim e minúsculas. Não há exigência adicional de domínio neste endpoint. Falha SMTP registra apenas mensagem fixa, sem destinatário, exceção SMTP ou token; o token não enviado é removido. O envio é síncrono e sujeito aos timeouts configurados; a resposta é genérica, mas não há garantia de tempo constante.

### POST /auth/redefinir-senha

Requisição:

```json
{"token":"TOKEN_DO_LINK_COM_43_CARACTERES_BASE64URL","novaSenha":"NovaSenha123!"}
```

O valor acima é ilustrativo: substituir pelo token exato recebido no link.

200:

```json
{"mensagem":"Senha alterada com sucesso. Entre com sua nova senha."}
```

400 para token desconhecido, expirado ou usado: {"mensagem":"Token inválido ou expirado.","status":400}, segundo o formato ErrorResponse existente. A senha exige pelo menos 8 caracteres, não pode ser apenas espaços e deve ter no máximo 72 bytes UTF-8 (limite BCrypt). Validações de DTO também retornam 400. A senha antiga deixa de funcionar; nenhuma senha é enviada por e-mail.

### Limites

Janelas fixas UTC de 15 minutos, contadores atômicos compartilhados no PostgreSQL, preservados após reinício:

- Solicitação: 10 por IP por janela; acima disso 429.
- Envio por endereço: 3 por janela; acima disso resposta genérica 200 sem novo envio, mesmo para contas ausentes.
- Redefinição: 20 por IP e 5 por token por janela; acima disso 429.

429: {"mensagem":"Muitas tentativas. Aguarde até 15 minutos.","status":429}. A contagem de redefinição inclui tentativas bem-sucedidas; entradas rejeitadas pela validação de DTO não consultam tokens. Chaves de limitação são hashes SHA-256, sem e-mail, IP ou token em claro. Contadores de janelas antigas são limpos nas chamadas. Janelas fixas permitem rajadas na virada da janela. Use proteção HTTP no proxy contra tráfego volumétrico e corpos inválidos.

O IP vem de getRemoteAddr(), sem confiar diretamente em X-Forwarded-For. Atrás de proxy configure a resolução de IP na infraestrutura para aceitar cabeçalhos apenas do proxy confiável; sem isso usuários podem compartilhar a cota do proxy.

## Integração React

1. Tela “Esqueci minha senha”: POST do e-mail; mostrar a mensagem genérica para 200, inclusive se a conta não existir. O api.js atual anexa JWT automaticamente: usar chamada pública sem Authorization nessas duas rotas.
2. O e-mail aponta para PASSWORD_RESET_URL#token=... . A tela lê o fragmento com URLSearchParams(location.hash.slice(1)), mantém o token apenas em memória e remove o fragmento com history.replaceState. Não salvar em localStorage, telemetria ou logs. Evitar scripts de terceiros nessa tela.
3. “Verificação do link”: verificar presença e formato de 43 caracteres Base64URL antes de mostrar o formulário. A validade real é verificada no POST final; não há endpoint separado de validação nem consumo em GET. Isso evita consumo por scanners de e-mail.
4. “Definição de nova senha”: validar também confirmação local e enviar token + novaSenha. Em 400 informar link inválido/expirado e oferecer nova solicitação. Em 429 pedir para aguardar.
5. “Confirmação”: em 200 apagar token e sessão locais e exibir confirmação com link de login. Não efetuar login automático. Demais sessões receberão 401 e precisarão entrar novamente.
6. Configurar fallback da hospedagem React para servir a rota /redefinir-senha. O frontend atual permanece inalterado e ainda informa indisponibilidade até essa integração.

## Arquivos

Novos: Controller/RecuperacaoSenhaController.java; DTOs/Request/EsqueciSenhaRequestDTO.java e RedefinirSenhaRequestDTO.java; Service/RecuperacaoEmailService.java, RecuperacaoLimiteService.java e RecuperacaoSenhaService.java; db/migration/V7__recuperacao_senha.sql; testes RecuperacaoSenhaTests.java e RecuperacaoEndpointTests.java; este documento.

Alterados: pom.xml (H2 somente para testes); Config/SecurityConfig.java e TokenServiceFilter.java; Exception/GlobalExpcetionHandler.java; Model/Usuario/UsuarioModel.java; Repository/UsuarioRepository.java; Service/TokenService.java; application.properties; AuthenticationTests.java.

## Verificação

Com Java 21 e Maven: mvnw.cmd -Dtest=AuthenticationTests,RecuperacaoSenhaTests,RecuperacaoEndpointTests test.

Testes de recuperação executam o serviço com JDBC real em H2, BCrypt real e repositório de usuário/e-mail simulados. Testes HTTP verificam contratos, validações, limitação e permissões; não enviam e-mail real. Contador PostgreSQL é simulado no teste unitário; entrega SMTP e aplicação da migration em PostgreSQL precisam da verificação de implantação.

Resultado nesta implementação: BUILD SUCCESS, 23 testes, zero falhas/erros/ignorados (AuthenticationTests: 10; RecuperacaoEndpointTests: 6; RecuperacaoSenhaTests: 7). Inclui e-mails cadastrado/ausente; tokens válido, expirado, inválido e reutilizado; invalidação dos demais pendentes; senha nova aceita e antiga rejeitada no serviço de login; JWT antigo e legado rejeitados pelo filtro; rotas públicas; validações e limites; falha SMTP; composição do e-mail; execução de V6 + V7 em H2 modo PostgreSQL com preservação do usuário e exclusão em cascata. Não foi executado ApplicationTests, que depende do ambiente completo.

O wrapper Maven local não iniciou neste ambiente; a suíte foi executada com a instalação Maven 3.9.11 e JDK 21 já disponíveis, em modo offline, usando as mesmas metas acima. Acesso às dependências exigiu execução fora da restrição de filesystem.
