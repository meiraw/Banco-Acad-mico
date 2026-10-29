# Fotos de perfil

USER e ADMIN podem abrir Seu perfil, clicar em Adicionar foto ou Trocar foto,
selecionar um JPG/PNG de até 2 MB, conferir a prévia e clicar em Salvar foto.
Cancelar descarta a prévia sem alterar a foto salva. O avatar é atualizado no perfil
e no cabeçalho. Os dados atuais da conta, incluindo o nome cadastrado, são consultados
ao carregar a sessão. A foto permanece disponível após sair e entrar novamente.

## Backend

- GET `/perfil`: retorna apenas os dados da conta autenticada e `possuiFoto`.
- GET `/perfil/foto`: retorna a foto da própria conta em PNG, com Cache-Control no-store.
- PUT `/perfil/foto`: recebe multipart com o campo `foto`, sem ID de usuário no contrato.
- As rotas exigem JWT e permitem USER e ADMIN. A conta é obtida pelo principal autenticado.
- A imagem é decodificada para validar o conteúdo real, limitada a 16 megapixels,
  reduzida proporcionalmente a no máximo 512 px e regravada em PNG sem os metadados originais.
- A migration V10 adiciona a tabela fotos_perfil com FK para usuarios. Não modifica
  registros existentes. A foto é opcional e a troca substitui o conteúdo anterior.
- As imagens ficam no PostgreSQL, sem necessidade de pasta adicional de uploads.

## Arquivos principais

- Controller/PerfilController.java, Service/FotoPerfilService.java e V10__fotos_perfil.sql.
- SecurityConfig.java: acesso autenticado limitado aos três endpoints acima.
- UsuarioResponseDTO.java: indicador possuiFoto para a consulta de perfil.
- GlobalExpcetionHandler.java: mensagem para requisição multipart acima do limite global.
- frontend/src/pages/Perfil.jsx: seleção, prévia, confirmação e mensagens.
- frontend/src/components/Avatar.jsx e AppShell.jsx: avatar no perfil e cabeçalho.
- frontend/src/lib/api.js e sessao.jsx: consulta autenticada, download binário e atualização da sessão.

## Verificação

43 testes de backend passaram, incluindo 6 novos testes de foto: upload/troca/leitura,
ADMIN, bloqueio anônimo, isolamento entre contas, arquivos inválidos/vazios/grandes e
compatibilidade/cascata da migration em H2. Também passaram 12 testes Node do cliente
e validações, e npm run build.

ApplicationTests passou com o PostgreSQL local e aplicou V10 com sucesso (44 testes
de backend considerando essa execução). Foi usada a senha do .env apenas no processo
de teste para evitar a variável DB_PASSWORD conflitante do ambiente de ferramentas.
Nenhuma credencial foi alterada. Não foi realizada verificação manual pelo navegador.

Reinicie o backend para carregar os novos endpoints e recarregue o frontend.
As migrations anteriores, o cadastro sem foto e os endpoints de login e recuperação
permanecem preservados. Nenhuma dependência adicional foi necessária.
