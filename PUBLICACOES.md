# Página de publicação

Ao clicar no título, prévia ou em Ver publicação, o usuário abre `/material/:id`.
A página exibe título, descrição, autor, data (quando registrada), arquivo e favoritos.
Os comentários aparecem abaixo do conteúdo, com os mais recentes primeiro e paginação
de 50 em 50. A publicação de comentários exige login e aceita até 2000 caracteres.
O autor é obtido da conta autenticada, tanto no envio de materiais como de comentários.

PDFs e imagens raster podem ser vistos na página. Os demais formatos oferecem download;
não há envio de documentos a visualizadores externos. Arquivos HTML/SVG não são executados
como páginas do projeto. O acesso aos arquivos segue a leitura pública de materiais já
configurada no backend; a interface da publicação exige a sessão existente.

A migration V11 adiciona autor/data opcionais aos materiais e a tabela de comentários,
sem alterar o conteúdo dos registros antigos. Materiais antigos sem autoria registrada
mostram “Autor não informado”. Novos envios registram autor e data automaticamente.
Comentários são removidos em cascata quando o respectivo material é excluído.

Os favoritos usam armazenamento local separado pelo ID de cada conta, neste navegador,
com sincronização entre cartões, página da publicação, lista de favoritos e abas da mesma
conta. Trocar de conta troca imediatamente a lista. Não são sincronizados entre dispositivos.
A chave global antiga é ignorada, pois não identifica quem marcou os materiais;
seu conteúdo não é atribuído automaticamente a nenhuma conta.

Reinicie o backend para aplicar a V11 e carregar os novos endpoints. A configuração e a
senha do PostgreSQL precisam estar corretas. O frontend usa o proxy `/api` existente.

Endpoints adicionados:

- GET `/material/{id}/arquivo`: visualização; `?download=true` solicita download.
- GET `/material/{id}/comentarios?pagina=0`: comentários paginados.
- POST `/material/{id}/comentarios`: JSON `{ "texto": "Comentário" }`, autenticado.

GET `/material/{id}` e a listagem agora incluem `autorNome` e `publicadoEm`, sem expor
e-mail, senha ou outros dados da conta do autor.

## Publicações no perfil

O perfil de USER e ADMIN exibe “Minhas publicações”, com cartões que mostram disciplina,
semestre e curso, e abrem a página de cada material. GET `/perfil/publicacoes?pagina=0`
exige login e consulta apenas o autor autenticado, em páginas de 12 materiais, com os
mais recentes primeiro. Não aceita escolher outro autor. Não requer nova migration.
Materiais antigos sem `autor_id` não são atribuídos automaticamente a nenhuma conta.

## Curtidas e respostas

A migration V12 adiciona curtidas por conta e respostas vinculadas aos comentários,
preservando materiais e comentários existentes. Curtir e descurtir são operações
idempotentes: cada conta tem no máximo uma curtida por material. O contador é público
na interface autenticada e separado dos favoritos locais. As respostas ficam abaixo
do comentário original, com autor e data; aceitam até 2000 caracteres e
têm paginação de 50 em 50. Somente contas autenticadas podem curtir ou responder.

- GET/PUT/DELETE `/material/{id}/curtida`: consultar, curtir e descurtir.
- GET/POST `/material/{id}/comentarios/{comentarioId}/respostas`: listar/publicar respostas.
- GET de comentários inclui `totalRespostas` para abrir a conversa desejada.

Reinicie o backend para aplicar V12 antes de usar os novos controles.

O autor de um comentário não pode responder à própria pergunta, inclusive quando é
ADMIN. O botão Responder fica disponível apenas nos comentários de outras contas,
e o backend também bloqueia tentativas diretas. O autor da publicação pode responder
a comentários de outras pessoas. Comentários incluem `autorId` para identificar essa
condição sem comparar nomes. A leitura das respostas existentes continua disponível.

A V13 permite continuar a conversa respondendo a uma resposta específica. O POST de
respostas aceita `respostaPaiId` opcional; ele deve pertencer à mesma conversa. A regra
de autoria considera o alvo escolhido: o autor da pergunta pode responder ao colega
que a respondeu, mas não à própria pergunta nem às próprias respostas. A interface
oferece Responder nas mensagens de outras pessoas e identifica “Em resposta a …”.
Reinicie o backend para aplicar V13 e carregar essa regra.
