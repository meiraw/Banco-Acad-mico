# Favoritos por conta

## Persistência e segurança

A migration `V19__favoritos.sql` adiciona `favoritos(usuario_id, material_id, criado_em)`.
Os IDs são UUIDs, o par usuário/material é a chave primária e as duas referências
usam `ON DELETE CASCADE`. Há índices para listar por usuário/data e para excluir
relações de um material. A versão 18 e a ausência da tabela foram confirmadas no
catálogo do PostgreSQL antes de criar a migration. Não se reutilizam curtidas ou dislikes.

`FavoritoService` resolve o usuário pelo `Principal` autenticado pelo JWT existente.
Não há parâmetro livre para escolher outra conta. A inclusão bloqueia o material na
mesma transação e exige `APROVADO`, inclusive para autor e administrador. Listas e IDs
também filtram aprovados. Inclusões repetidas são idempotentes e a chave composta
impede duplicação. Remover um favorito já ausente retorna 204 sem revelar o material.

O repository da relação usa `JdbcTemplate`, seguindo o padrão de interações do
projeto. A consulta paginada de materiais usa o `MaterialRepository` existente.
Não foram necessárias novas entidades JPA ou dependências de produção.

## API

Todas as rotas exigem USER ou ADMIN. O navegador usa `/api` como prefixo do proxy;
o backend recebe os caminhos abaixo sem esse prefixo.

| Método | Caminho | Resultado |
| --- | --- | --- |
| GET | `/favoritos?page=0&size=12` | Materiais paginados; metadados no padrão VIA_DTO |
| GET | `/favoritos/ids?page=0&size=200` | UUIDs paginados para o estado dos corações |
| PUT | `/favoritos/{materialId}` | 204; garante que esteja favoritado |
| DELETE | `/favoritos/{materialId}` | 204; garante que não esteja favoritado |

Páginas começam em zero e aceitam de 1 a 200 itens. A ordem usa data decrescente e
UUID para desempate. Sem autenticação: 401. Identificador ou paginação inválidos: 400.
Material inexistente ou não aprovado na inclusão: 404. Consultas usam cache privado
sem armazenamento. Os endpoints não expõem favoritos ou dados privados de outra conta.

## React

`useFavoritos` compartilha uma instância de estado por ID/token da sessão.
`favoritos.js` consulta todas as páginas de IDs sem carregar o catálogo de materiais;
as alterações visuais são confirmadas após o servidor responder. Requisições repetidas
do mesmo material ficam bloqueadas enquanto pendentes. Respostas atrasadas da sessão
anterior são descartadas. Uma consulta antiga não sobrescreve uma alteração confirmada.

A lista de favoritos busca sua própria página de materiais. Alterações confirmadas,
abertura da página, foco da janela e retorno da aba visível provocam atualização.
Não há push/WebSocket: outro dispositivo vê as mudanças ao consultar novamente.
Erros são apresentados com opção de tentar novamente. Excluir uma publicação apenas
invalida o estado de favoritos; nunca alterna um favorito depois de excluir o material.

Os favoritos são consultados exclusivamente pela API. O frontend não lê nem grava
favoritos no localStorage e não oferece aviso, botão ou rotina de importação.
Dados antigos eventualmente presentes no navegador são ignorados, sem apagá-los.
Os favoritos já salvos no PostgreSQL permanecem disponíveis normalmente.

## Inicialização e testes

Reiniciar o backend com sua conexão PostgreSQL válida aplica a V19 pelo Flyway e
carrega os novos endpoints. Apenas criar a migration não atualiza um processo Java
já em execução. Não aplique o SQL manualmente sem o histórico Flyway.

`FavoritosTests` usa H2 em memória por padrão. Para testar SQL e concorrência reais,
aceita `FAVORITOS_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:PORTA/favoritos_test` e
`FAVORITOS_TEST_DB_PASSWORD`, exclusivamente em banco de teste isolado. O teste recusa
outros nomes de banco/hosts nesse modo e aplica migrations somente ao banco de teste.

A cobertura inclui autenticação, UUID/paginação inválidos, isolamento USER/ADMIN,
idempotência, concorrência, integridade referencial, exclusão em cascata, materiais
restritos, paginação e clientes independentes. Os testes Node verificam o contrato
da API, respostas atrasadas, falhas de conexão e independência do armazenamento local.
