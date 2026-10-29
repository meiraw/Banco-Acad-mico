# Cadastro acadêmico

## Comportamento

O cadastro em `/cadastro`, acessível pelo botão Criar conta do login em `/auth`, apresenta nome completo, e-mail,
curso fixo Ciência da Computação, semestre atual (1º ao 8º), senha e confirmação.
A confirmação fica apenas no navegador. O servidor define o curso e o perfil USER.
A senha continua sendo armazenada com BCrypt. O limite de 72 bytes do BCrypt também
é verificado, além do mínimo de 8 caracteres. O medidor de força é orientativo.

O fluxo existente foi preservado: POST `/usuario`, seguido de POST `/auth/login`
e abertura do painel. Se o cadastro concluir e o login automático falhar, a interface
informa que a conta já foi criada e permite entrar sem repetir o cadastro.
Há bloqueio de envios duplicados, campos desabilitados e mensagem de carregamento.

`semestreAtual` é obrigatório para novos cadastros. O curso não é recebido no DTO:
mesmo que seja enviado um valor diferente, o servidor usa o curso fixo.
O DTO de atualização administrativa permanece separado e não exige campos novos.

## Arquivos alterados e criados

- `frontend/src/pages/Auth.jsx`: campos, ordem, validações, carregamento e integração.
- `frontend/src/lib/cadastro.js`: curso fixo e validação do cadastro no frontend.
- `frontend/src/lib/cadastro.test.js`: testes de campos e confirmação de senha.
- `frontend/src/lib/cadastro-api.test.js`: testes do cliente HTTP e tratamento de respostas.
- `src/main/java/br/wm/banco/academico/DTOs/Request/CadastroUsuarioRequestDTO.java`: contrato exclusivo do cadastro.
- `src/main/java/br/wm/banco/academico/Controller/UsuarioController.java`: novo DTO no POST existente.
- `src/main/java/br/wm/banco/academico/Service/UsuarioService.java`: gravação dos dados acadêmicos e limite BCrypt.
- `src/main/java/br/wm/banco/academico/Model/Usuario/UsuarioModel.java`: curso fixo e semestre atual.
- `src/main/java/br/wm/banco/academico/DTOs/Response/UsuarioResponseDTO.java`: dados acadêmicos na resposta, sem senha.
- `src/main/resources/db/migration/V9__cadastro_academico.sql`: novos campos e restrição de intervalo.
- `src/test/java/br/wm/banco/academico/AuthenticationTests.java`: novos cenários e adaptação das requisições de cadastro.
- `src/test/java/br/wm/banco/academico/CadastroAcademicoMigrationTests.java`: preservação do usuário antigo e validação do intervalo no banco.
- `CADASTRO_ACADEMICO.md`: este relatório.

## Banco de dados

A V9 adiciona `curso VARCHAR(200) NOT NULL DEFAULT 'Ciência da Computação'` e
`semestre_atual INTEGER`, com CHECK de 1 a 8. Contas anteriores recebem o curso fixo
e mantêm semestre nulo, pois essa informação não era conhecida.
Nenhuma migration anterior foi modificada, nem foram excluídos registros ou relacionamentos.
A V9 foi aplicada com sucesso ao PostgreSQL local durante o teste de inicialização.

## Validação executada

- 37 testes de backend aprovados: AuthenticationTests (17), RecuperacaoSenhaTests (7),
  RecuperacaoEndpointTests (6), ConfirmacaoEmailTests (6), CadastroAcademicoMigrationTests (1).
- ApplicationTests aprovado com PostgreSQL local: Flyway aplicou V9 e Hibernate validou os mapeamentos.
  Total final de backend: 38 testes aprovados.
- 8 testes aprovados pelo Node, incluindo subtestes do cliente HTTP.
- `npm run build` aprovado.
- `git diff --check` sem erros.

Cobertura: cadastro válido, e-mail duplicado e inválido, nome vazio, senha curta,
confirmação ausente ou diferente, semestre ausente ou fora do intervalo, limites 1 e 8,
curso fixo apesar de alteração na requisição, USER apesar de tentativa de ADMIN,
BCrypt, normalização do e-mail, login após cadastro, atualização de conta antiga,
preservação de senha/perfil/id após a migration, erros HTTP e falha de conexão.

O primeiro ApplicationTests falhou porque DB_PASSWORD no ambiente de teste diferia
do POSTGRES_PASSWORD do .env. A repetição com a senha do .env apenas no processo
de teste passou. Nenhuma credencial foi alterada. Variáveis do ambiente prevalecem
sobre o .env: se o erro aparecer ao iniciar, confira DB_PASSWORD na configuração de execução.

## Pendências e limites

- Reiniciar o backend em execução para carregar as classes atualizadas e recarregar o frontend.
- Não foi realizada verificação visual nem cadastro manual pelo navegador nesta etapa.
- Os testes HTTP do cadastro usam repositório simulado. A inicialização/migration foi
  validada no PostgreSQL real, sem criar contas de teste no banco do projeto.
- O teste opt-in SmtpRealTests não foi repetido, pois esta alteração não modifica o SMTP.
- Contas antigas continuam entrando sem informar semestre. Não foi acrescentada interface
  para completar esse dado, pois isso está fora do formulário de cadastro solicitado.
- Login, recuperação de senha, JWT, segurança, módulos acadêmicos e administração
  não tiveram endpoints ou permissões modificados. Nenhuma dependência foi adicionada.

Para repetir os testes do frontend na pasta frontend:

```text
node --test src/lib/cadastro.test.js src/lib/cadastro-api.test.js
npm run build
```

Para repetir os testes isolados do backend com Java 21 e Maven:

```text
mvnw.cmd -Dtest=AuthenticationTests,RecuperacaoSenhaTests,RecuperacaoEndpointTests,ConfirmacaoEmailTests,CadastroAcademicoMigrationTests test
```

O wrapper apresentou falha no ambiente desta sessão. Foi usado o Maven 3.9.11 já
instalado, em modo offline e com o repositório local de dependências.
