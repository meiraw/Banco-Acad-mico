# Fórum Acadêmico

## Navegação e funcionamento

A barra lateral autenticada oferece Perfil, Início, Fórum, Favoritos, Configurações e Sair. Início reutiliza `/painel`; o fórum usa `/forum`, `/forum/nova` e `/forum/:id`. O item ativo é indicado também nos detalhes. A barra continua fixa no desktop e acompanha o fluxo nas telas menores. As páginas inicial pública e Dashboard permanecem sem botão Voltar.

Estudantes autenticados podem criar discussões por disciplina, responder e editar seu próprio conteúdo. Administradores podem remover conteúdos de qualquer autor, mas não reescrever textos de terceiros. Não há aprovação prévia, respostas encadeadas, anexos, curtidas ou notificações no fórum. O upload e os comentários dos materiais continuam independentes.

Título: 200 caracteres; discussão: 10.000; resposta: 5.000. Campos obrigatórios são verificados no React, DTOs e service, inclusive textos compostos apenas por espaços. O autor vem da identidade autenticada no backend, nunca de um ID escolhido no formulário. O frontend apresenta texto como conteúdo React, sem interpretar HTML.

## API

Todos os endpoints exigem JWT com papel USER ou ADMIN. O frontend chama `/api/forum/...`; o proxy encaminha `/forum/...` ao backend.

| Método | Endpoint | Operação |
| --- | --- | --- |
| GET / POST | `/forum/discussoes` | Listar / criar |
| GET / PUT / DELETE | `/forum/discussoes/{id}` | Detalhar / editar / excluir logicamente |
| GET / POST | `/forum/discussoes/{id}/respostas` | Listar / responder |
| PUT / DELETE | `/forum/discussoes/{id}/respostas/{respostaId}` | Editar / excluir resposta |

Discussão recebe `titulo`, `disciplinaId` e `conteudo`; resposta recebe `conteudo`. Edições também exigem `versao`, obtida na consulta anterior. Versão desatualizada retorna 409. Somente o autor pode editar. Autor ou administrador pode excluir. Uma resposta de outra discussão não pode ser manipulada pela URL errada.

Listagem: `q` (até 200 caracteres), `disciplinaId`, `minhas`, `ordem=recentes|antigas`, `page` e `size`. A pesquisa abrange título, conteúdo e nome da disciplina, sem diferenciar maiúsculas. `%`, `_` e `!` são tratados como caracteres literais. Não há garantia de equivalência entre palavras com/sem acento. Consultas são parametrizadas; não há ordenação SQL livre.

Paginação padrão: 20; máximo: 50; páginas de 0 a 10.000. A ordenação usa data e UUID como desempate. Respostas aparecem da mais antiga à mais recente. A API segue o contrato `content` + `page` já existente. O total de respostas ignora respostas excluídas e não depende da página exibida. A listagem usa projeção com contagem no banco para evitar carregar todas as conversas.

## Persistência e exclusão

`V20__forum_academico.sql` cria `discussoes_forum` e `respostas_forum`, com UUIDs, chaves estrangeiras, validações básicas e índices. Migrations antigas não foram alteradas. Não há dados demonstrativos inseridos pela migration nem novas dependências.

As entidades JPA usam versionamento otimista. Alterações de respostas e exclusão da discussão bloqueiam a mesma discussão dentro da transação, evitando respostas que reabram uma conversa removida.

Exclusão é lógica (`excluido_em`): o registro permanece armazenado. Uma discussão excluída e suas respostas ficam indisponíveis inclusive para novas respostas. Não há exclusão em cascata das respostas. Não foi criada interface de restauração ou política de eliminação definitiva.

Excluir uma conta preserva as conversas e torna o autor nulo, apresentado como “Conta excluída”. Disciplinas com histórico, inclusive discussões removidas logicamente, não podem ser excluídas; a API retorna 409. Isso preserva o vínculo acadêmico e é uma mudança intencional na exclusão administrativa de disciplinas.

## Ativação

Ao reiniciar o backend atualizado, o Flyway aplicará V20 no banco configurado. A validação desta implementação aplicou a migration somente em bancos de teste; não iniciou nem reiniciou a instância principal ou aplicou manualmente a migration ao banco real. O frontend Vite atualiza as telas durante o desenvolvimento; em implantação, gere e publique o build conforme o processo existente.

## Testes

`ForumTests` cobre autenticação, autoria, validação, pesquisa, filtros, paginação, edição, versão desatualizada, respostas, contagem, exclusão lógica, moderação administrativa, exclusão de conta, proteção de disciplinas e concorrência entre resposta/exclusão.

Por padrão usa H2 isolado. Para PostgreSQL, exige `FORUM_TEST_JDBC_URL` no formato `jdbc:postgresql://127.0.0.1:PORTA/forum_test` e `FORUM_TEST_DB_PASSWORD`, com banco descartável exclusivo. Nunca apontar testes para o banco de desenvolvimento ou produção.

`frontend/src/lib/forum-api.test.js` cobre contratos, Bearer, parâmetros, paginação e erros de conexão/conflito. As suítes existentes verificam regressão de autenticação, materiais, upload, favoritos e moderação. SMTP externo e `ApplicationTests` são excluídos da execução segura.

Resultados em 12/11/2026:

- Fórum inicialmente em H2: 15 testes aprovados.
- Regressão do backend, com o fórum em PostgreSQL 17 descartável e as demais suítes isoladas: 150 testes, 149 aprovados e 1 ignorado por falta de permissão para links simbólicos; zero falhas ou erros.
- Após acrescentar o teste de paginação de respostas: 16 testes do fórum aprovados em PostgreSQL 17.
- Frontend: 36 testes aprovados; build Vite concluído com sucesso.

Comandos principais: `mvn -Dtest=*Tests,!ApplicationTests,!SmtpRealTests -Dsmtp.real=false -Dspring.profiles.active=test test`, `mvn -Dtest=ForumTests -Dsmtp.real=false -Dspring.profiles.active=test test`, `node --test --test-reporter=tap src/lib/*.test.js` e `npm run build`. Foi utilizado o Maven instalado, pois o wrapper local não inicia neste ambiente. A execução PostgreSQL usou somente as variáveis de teste indicadas acima. Nenhuma dependência foi instalada e nenhum commit foi criado.

A verificação visual no navegador foi bloqueada pela ferramenta de controle do computador, que não conseguiu determinar a URL com segurança. Não foi contornado o bloqueio. Desktop, notebook, tablet e celular precisam de conferência visual manual; o build e os testes de API não substituem essa conferência. A imagem de referência mencionada não estava anexada; foram reutilizados os estilos e a organização descritos.
