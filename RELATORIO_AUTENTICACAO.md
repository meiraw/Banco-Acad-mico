# Autenticação e controle de acesso

## Etapa 1 — Código existente

Foram inspecionados controllers, services, repository, DTOs, entidade, configuração de segurança, migrations, testes e consumo da API pelo React. Não foram encontrados arquivos AGENTS.md no projeto. O Git estava sem alterações no início.

Já existiam cadastro com BCrypt e perfil USER obrigatório, validação dos DTOs, verificação de e-mail duplicado, login por e-mail/senha, geração HMAC256 com emissor e validade de duas horas, enum USER/ADMIN e respostas sem senha. Essas implementações foram reaproveitadas. UsuarioService, controllers, DTOs, entidade, repository e migrations não foram modificados.

Faltavam validação JWT, autenticação na cadeia Spring Security, autorização por perfil e uma chave externa. SecurityConfig liberava todas as requisições. O frontend simulava login localmente e não enviava credenciais ao backend.

## Etapa 2 — Implementação

- TokenService conserva gerarToken e acrescenta validarToken: assinatura HMAC256, emissor, expiração obrigatória e subject obrigatório não vazio.
- A chave fixa foi removida. JWT_SECRET é obrigatório, com pelo menos 32 bytes; utilize um segredo aleatório forte. Tokens antigos deixam de funcionar após a troca da chave.
- TokenServiceFilter autentica Authorization: Bearer, consulta o usuário existente e utiliza seu perfil atual no banco. Não confia no claim de perfil para autorizar.
- Segurança stateless, sem login por formulário ou cookies de sessão; CSRF desativado para o fluxo Bearer. Respostas 401/403 em JSON com mensagem e status.
- CORS aceita por padrão http://localhost:5173 e pode ser configurado por CORS_ALLOWED_ORIGINS, com origens separadas por vírgulas.
- React utiliza POST /usuario e POST /auth/login e mantém o formato de resposta {token}. Envia Bearer também em multipart sem sobrescrever seu Content-Type. JWT e sessão ficam em sessionStorage, são removidos no logout e expiram automaticamente. A sessão antiga simulada é descartada. A rota /admin exige ADMIN no frontend; a autorização efetiva sempre ocorre no backend.
- Cadastro no formulário exige ao menos oito caracteres, como o DTO existente; envio duplicado é bloqueado enquanto a requisição está em andamento. Recuperação de senha agora informa que está indisponível, em vez de afirmar envio inexistente.

### Permissões adotadas

| Operação existente | Permissão |
| --- | --- |
| POST /usuario e POST /auth/login | Pública |
| GET do catálogo: /curso, /semestres, /disciplinas, /material e subcaminhos | Pública, preservando a consulta existente |
| POST /material | USER ou ADMIN |
| Gestão de usuários: GET/PUT/DELETE /usuario e subcaminhos | ADMIN |
| Escritas em cursos, semestres e disciplinas | ADMIN |
| PUT/DELETE de materiais | ADMIN |
| Demais requisições | Negadas |

Os endpoints de usuário não possuem propriedade de recurso para autosserviço; a restrição administrativa evita que um USER altere contas de terceiros. Nenhum endpoint novo foi criado. O cadastro público continua sempre criando USER, mesmo se o cliente enviar perfil ADMIN. Não foi criada promoção pública a administrador.

### Arquivos modificados ou adicionados

- src/main/java/br/wm/banco/academico/Service/TokenService.java
- src/main/java/br/wm/banco/academico/Config/SecurityConfig.java
- src/main/java/br/wm/banco/academico/Config/TokenServiceFilter.java (novo)
- src/main/java/br/wm/banco/academico/Config/SecurityResponse.java (novo)
- src/main/resources/application.properties
- frontend/src/lib/api.js
- frontend/src/lib/sessao.jsx
- frontend/src/pages/Auth.jsx
- frontend/src/components/RequireAuth.jsx
- frontend/src/App.jsx
- src/test/java/br/wm/banco/academico/AuthenticationTests.java (novo)
- RELATORIO_AUTENTICACAO.md (novo)

## Etapa 3 — Verificação

- `./mvnw.cmd -DskipTests compile`: aprovado.
- `./mvnw.cmd -B -Dtest=AuthenticationTests test`: nove testes aprovados, sem falhas ou erros.
- Testes HTTP com MockMvc, controllers e cadeia real Spring Security, serviços de cadastro/login e BCrypt reais; repository simulado, sem alterar PostgreSQL. MaterialService simulado para verificar autorização sem gravar arquivos.
- Cobertura: cadastro e BCrypt, perfil USER obrigatório, senha ausente na resposta, duplicidade e validação de dados, login válido/inválido, JWT emitido e validado, token malformado/expirado/com emissor ou assinatura incorretos/sem expiração, ausência de autenticação, USER proibido, ADMIN permitido, alteração de perfil no banco, usuário removido, CORS permitido/proibido, consulta pública, envio autenticado e exclusão administrativa de material, bloqueio de escritas do catálogo a USER.
- A verificação de envio de material confirma acesso do USER ao controller, que retorna 400 por ausência das partes multipart; não representa teste completo de upload e persistência.
- Dependências React instaladas com `npm ci --no-audit --no-fund`, respeitando package-lock. `npm run build`: aprovado.
- `git diff --check`: sem erros de whitespace.

## Configuração e pendências

Defina JWT_SECRET no ambiente do processo Java antes de iniciar, além do DB_PASSWORD já exigido. Um arquivo .env não é carregado automaticamente pelo Spring. Mantenha o segredo fora do Git. Em produção, configure as origens reais em CORS_ALLOWED_ORIGINS e o proxy /api; o proxy de desenvolvimento continua apontando para localhost:8081, enquanto o Spring usa 8080 por padrão. Ajuste a porta de inicialização ou o proxy conforme seu ambiente.

Ainda é necessária uma verificação integrada com PostgreSQL e navegador no ambiente de execução. O teste preexistente ApplicationTests/contextLoads não foi executado: carrega a configuração real, inclusive banco/Flyway. Os testes desta entrega não verificam persistência real nem executam migrations. Nenhum dado, estrutura ou migration foi alterado.

O administrador inicial precisa existir e ter sido provisionado por um procedimento confiável. Nenhuma conta ou promoção foi criada automaticamente. Login inválido mantém o status 409 do tratamento de negócio existente para preservar o contrato; falhas de autenticação Bearer retornam 401.

Recuperação de senha continua sem backend. GET /material/{id}/arquivo, já referenciado pelo frontend, não existe no controller; não foi criado por estar fora do escopo. Login não devolve nome: para contas já existentes, o frontend usa a parte anterior ao @ como rótulo, mantendo a resposta {token}. Os atalhos administrativos existentes podem continuar visíveis a USER, mas a rota e a API bloqueiam acesso. Não há revogação individual de JWT no logout; ele remove a sessão do navegador, e o token expira em duas horas. Não foi acrescentada infraestrutura de revogação ou limitação de tentativas.

Não foram executados commits nem push.
