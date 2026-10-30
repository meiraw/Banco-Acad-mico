# Isolamento entre contas

As preferências são separadas pelo ID da conta, sem usar o papel USER/ADMIN como
identificador. Dois usuários USER também possuem preferências independentes.

- Tema: `banco-academico:tema:{id}`. O visitante usa um espaço separado. A chave global
  antiga não é importada, pois não indica de quem era a preferência.
- Favoritos: `banco-academico:favoritos:{id}`. As marcações não passam de uma conta a outra.
- Essas preferências são locais ao navegador; não há sincronização entre dispositivos.
- O token e os dados da sessão usam sessionStorage, separado por aba. Dados em cache
  só são reutilizados quando pertencem ao e-mail identificado pelo token ativo.
- Entrar/sair remonta a interface, descartando formulários, prévias, filtros e outros
  estados temporários da conta anterior. Respostas atrasadas do perfil são descartadas
  quando a sessão muda. A identificação da conta independe do download da foto.
- Enquanto o ID da conta está carregando, os controles pessoais não gravam preferências
  no espaço do visitante nem no de outra conta.
- O tema é aplicado por um único provider, inclusive ao navegar e recarregar a página.
  Eventos de outras contas em outras abas não alteram o tema da conta atual.

No backend, foto e publicações pessoais usam a identidade autenticada. Curtidas são
armazenadas por material e usuário; comentários e respostas registram o autor autenticado.
A leitura de materiais, comentários, respostas e do total de curtidas é compartilhada
por ser conteúdo colaborativo; cada mensagem mantém a autoria e cada curtida pertence
somente à conta que a fez. Alterações administrativas na grade são globais por definição.

Não houve alteração de permissões ou migração de banco nesta correção. Os testes de
regressão cobrem troca entre ADMIN, dois USERs e visitante, retorno à conta, favoritos,
cache de sessão incompatível, foto e interações autenticadas.
