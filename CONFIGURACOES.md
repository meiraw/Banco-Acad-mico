# Configurações

## Interface

`/configuracoes` oferece três cartões: Conta e perfil, Aparência e Informações e suporte. São reutilizados AppShell, cores, bordas, botões, logo e rodapé. Configurações permanece sem botão Voltar e o item da barra lateral continua ativo nas subrotas.

- `/configuracoes/perfil`: editar somente o semestre entre os dados cadastrais; nome, curso e e-mail são somente para consulta. O recurso separado de foto foi preservado.
- `/configuracoes/senha`: alterar a senha com confirmação da senha atual.
- `/sobre` e `/termos`: conteúdos existentes preservados.
- `/privacidade` e `/ajuda`: informações públicas, acessíveis sem login. Quando há sessão, usam o layout autenticado.

O bloco de foto foi extraído de Perfil para `FotoPerfil.jsx`, compartilhado com Editar perfil. Seus endpoints e validações foram preservados. Para estudantes, foto e nome da barra lateral não abrem mais o antigo Perfil; a edição fica em Configurações. Minhas publicações usa `/minhas-publicacoes`, com botão abaixo de Fórum, e recebe o redirecionamento após enviar material. O administrador mantém o Perfil e sua área administrativa, sem o novo item do estudante. A rota antiga `/perfil` redireciona estudantes para a edição em Configurações.

## Dados pessoais

`PUT /perfil`, autenticado com USER ou ADMIN, aceita somente `semestreAtual`, obrigatório entre 1 e 8. A conta vem do Principal autenticado; nenhum identificador de conta é recebido na rota ou no DTO. Nome, curso, e-mail, senha, papel e versão de credenciais não são atualizados nesse endpoint, mesmo que enviados em campos adicionais. A alteração administrativa de usuários é independente desta tela.

Os campos existentes de usuários são reutilizados. Não há nova migration ou dependência. Após salvar, o frontend chama `recarregarPerfil()` para atualizar a sessão e a barra lateral. Se a atualização da visualização falhar depois da persistência, informa que os dados já foram salvos.

Foto e dados pessoais têm salvamentos separados. JPG e PNG de até 2 MB continuam sendo validados pelo serviço existente; a imagem é reprocessada e armazenada no PostgreSQL conforme o fluxo anterior.

## Alteração autenticada de senha

`PUT /perfil/senha` recebe `senhaAtual`, `novaSenha` e `confirmacaoNovaSenha`. A nova senha exige pelo menos 8 caracteres, até 72 bytes UTF-8 e confirmação idêntica. Texto composto apenas por espaços é rejeitado. Espaços em uma senha válida são preservados; não há trim das senhas.

A conta é bloqueada na transação, a senha atual é conferida com PasswordEncoder e a nova senha é armazenada com BCrypt. A operação incrementa a versão de credenciais existente e revoga recuperações de senha pendentes da conta. Não altera o formato ou filtro JWT. Após sucesso, todos os JWTs anteriores, inclusive o da sessão atual, ficam inválidos. O frontend limpa a sessão e retorna ao login com mensagem de sucesso. Essa operação não envia e-mail e é independente da recuperação por código.

Limites de 15 minutos reutilizam `limites_recuperacao` e `RecuperacaoLimiteService`: 20 operações por IP e 5 por conta. Os contadores são confirmados antes da transação da senha; falhas de senha atual não apagam tentativas. Chaves são hashes e possuem prefixos exclusivos desta operação. Violações de DTO anteriores ao controller não executam a conferência da senha nem esses contadores. O IP utiliza getRemoteAddr, seguindo o padrão existente.

Senha atual incorreta retorna 400; limite excedido retorna 429. Falhas de validação não alteram a conta ou seus tokens. Nenhuma senha é adicionada a logs ou respostas de sucesso.

## Tema

As preferências `claro`, `escuro` e `automatico` usam as chaves existentes de localStorage por conta; visitante tem preferência própria. Valores claro/escuro anteriores permanecem válidos. Automático usa `prefers-color-scheme: dark` e escuta mudanças enquanto selecionado. Sem suporte à preferência do dispositivo, usa claro.

O contexto distingue preferência e tema aplicado. `.dark` continua controlando a paleta existente. O botão rápido de tema considera o tema aplicado e escolhe explicitamente claro ou escuro, saindo do automático. Os eventos locais e storage mantêm sincronização entre componentes e abas. Armazenamento bloqueado gera mensagem na página de Configurações sem fingir que a escolha foi persistida.

## Privacidade e ajuda

Privacidade descreve somente recursos implementados: dados cadastrais, hash de senha, foto, conteúdo publicado, favoritos, sessão no navegador, aparência e operações de e-mail. Identifica como pendentes as definições de responsável, contato, bases legais, conservação, hospedagem, provedores, backups e procedimentos para titulares. Não estabelece prazos ou atendimento inexistente nem afirma que constitui uma política completa. O responsável pela plataforma deve completar essas informações antes de tratar a página como política definitiva.

Ajuda explica cadastro, acesso, recuperação, perfil, compartilhamento, pesquisa/filtros, favoritos, fórum e moderação. Não cria atendimento, chatbot ou canal de contato fictício.

## Verificação

`ConfiguracoesTests` cobre autorização, atualização apenas da própria conta, campos restritos, dados inválidos, contas antigas sem semestre, senha atual incorreta, confirmação, tamanho UTF-8, preservação de espaços, BCrypt, invalidação de sessões, novo login e limites de tentativas.

Usa H2 por padrão. A execução PostgreSQL é restrita a `CONFIG_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:PORTA/configuracoes_test` e `CONFIG_TEST_DB_PASSWORD`, exclusivamente em banco descartável. Em PostgreSQL, os testes executam o SQL real dos contadores; no H2, simulam os contadores. E-mails são simulados.

Testes Node cobrem contratos dos endpoints, Bearer, erros, preservação de senhas e preferências de tema, incluindo eventos do dispositivo, remoção de listeners, isolamento por conta e armazenamento indisponível. Os testes anteriores de foto, autenticação, recuperação, upload, Dashboard e fórum compõem a regressão.

Os testes de renderização HTML em `configuracoes-telas.test.js` verificam ordem dos cartões, destinos, semântica dos campos, opções de tema, destaque da navegação e acesso público às informações. Utilizam React e esbuild já instalados, com sessão fictícia e limpeza não recursiva dos arquivos temporários. Não substituem teste interativo no navegador ou conferência visual da responsividade, que permanecem pendentes nesta etapa.

Resultados em 12/11/2026:

- Regressão do backend: 161 testes, 160 aprovados e 1 ignorado por falta de permissão para criar links simbólicos; zero falhas ou erros. Configurações executou em PostgreSQL 17 descartável; demais suítes em ambientes isolados.
- Após reforço da validação de espaços Unicode e correção do isolamento dos dados de teste: 10 testes de Configurações aprovados novamente no PostgreSQL 17.
- Frontend: 42 testes aprovados; os 3 testes de renderização também foram repetidos com sucesso após ajuste da limpeza temporária.
- Build Vite concluído com sucesso.

Comandos: `mvn -Dtest=*Tests,!ApplicationTests,!SmtpRealTests -Dsmtp.real=false -Dspring.profiles.active=test test`, `mvn -Dtest=ConfiguracoesTests -Dsmtp.real=false -Dspring.profiles.active=test test`, `node --test --test-reporter=tap src/lib/*.test.js` e `npm run build`. Foi utilizado o Maven já instalado, porque o wrapper local não inicia neste ambiente. Não foram enviados e-mails reais nem criados commits.

Para ativar os novos endpoints no ambiente principal, reinicie o backend atualizado. Não há migration nova a aplicar. Nenhum serviço principal foi reiniciado por esta implementação e os testes não utilizam contas ou dados reais.
