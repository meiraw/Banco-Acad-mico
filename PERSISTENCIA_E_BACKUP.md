# Persistência, migração e backups

Esta etapa prepara os dados. Não publica o sistema nem move os uploads existentes.
O backend ainda será incluído no Compose na etapa de deploy. Até lá, continua
usando `Uploads` local; o volume Docker preparado não recebe esses arquivos automaticamente.

## Armazenamento portátil

Novos materiais guardam somente o nome interno do arquivo, resolvido dentro de
`UPLOAD_ROOT`. Caminhos absolutos da raiz atual continuam aceitos. Fotos de perfil
ficam na tabela `fotos_perfil`, incluída no backup PostgreSQL.

Ao importar banco e arquivos de outra máquina, configure `UPLOAD_LEGACY_ROOTS`
com o diretório antigo exato. Separe múltiplas raízes com `;`. Por exemplo:

```text
UPLOAD_ROOT=/data/uploads
UPLOAD_LEGACY_ROOTS=C:/diretorio-antigo/Uploads;/srv/instalacao-anterior/Uploads
```

O servidor usa somente o nome do arquivo sob essas raízes explícitas, nunca acessa
o diretório antigo. Não há alteração em registros ou migrations. Outros caminhos,
subpastas e traversal são recusados. Se houver duas origens com o mesmo nome de
arquivo e conteúdos diferentes, interrompa a importação: não sobrescreva arquivos.
Nunca aponte Nginx diretamente para uploads: o backend precisa aplicar a moderação.

## Volumes Docker preparados

- `dados-academicos`: PostgreSQL, já existente no Compose.
- `arquivos-academicos`: materiais, montado em `/data/uploads` no serviço de manutenção.
- `assinaturas-antimalware`: assinaturas do scanner.

`x-armazenamento-backend` documenta o volume e o ambiente que o futuro serviço
backend deverá utilizar. Uma extensão `x-` não inicia serviço nem faz montagem
por conta própria. Na etapa de deploy, também será necessário ajustar a propriedade
do volume ao UID do backend, sem dar permissão de escrita a outros usuários.

Reiniciar/recriar contêineres preserva volumes. **Não execute `docker compose down -v`
nem remova volumes com dados.** Volumes locais não protegem contra perda da VPS.

## Backup consistente

Scripts destinados a Linux com Bash, GNU tar, sha256sum e Docker Compose v2.
Não exigem instalação de pacotes da aplicação. Execute a partir da raiz do projeto.
O PostgreSQL deve estar rodando, mas pare todos os processos que escrevem no banco
ou nos uploads, inclusive o Spring Boot local/IntelliJ. Mantenha-os parados até o
término. A confirmação `--aplicacao-parada` é obrigatória; o script consegue detectar
o serviço chamado `backend`, mas não verifica processos externos.

Para a situação atual, com arquivos na pasta local:

```bash
mkdir -p backups
bash scripts/backup.sh --aplicacao-parada backups/antes-da-migracao ./Uploads
```

Após o deploy, com os arquivos já importados no volume:

```bash
bash scripts/backup.sh --aplicacao-parada backups/2026-11-14 --volume-docker
```

Se usar outro nome de projeto Compose, exporte `COMPOSE_PROJECT_NAME` explicitamente
com o mesmo nome empregado na instalação. O script recusa destino existente e
recusa backup de volume inexistente. Não aceita arquivos especiais ou links nos uploads.
Também confere se cada material do banco tem seu nome de arquivo na cópia de uploads.
Arquivos `CONCLUIDO`, `SHA256SUMS`, `banco.dump` e `uploads.tar.gz` compõem um backup.
Sem `CONCLUIDO`, a cópia está incompleta e não deve ser usada.

O dump lógico inclui fotos, materiais e histórico Flyway; não reaplique migrations
antigas manualmente após restaurá-lo. Não é uma cópia física do diretório PostgreSQL,
o que facilita a transferência Windows/Linux e AMD64/ARM64. Valide a restauração
na versão PostgreSQL 17 de destino antes de depender da cópia.

## Restauração de teste, sem tocar na instalação atual

Use somente backups confiáveis: um dump pode conter SQL executável. Os hashes
detectam corrupção, mas não autenticam a origem. Defina no ambiente uma senha
exclusiva em `RESTORE_POSTGRES_PASSWORD`; não a escreva no Git ou histórico do shell.

```bash
bash scripts/restaurar-teste.sh backups/antes-da-migracao banco-restauracao-teste01
```

O comando verifica os hashes e os caminhos do arquivo compactado antes de criar
recursos; exige nome novo com prefixo `banco-restauracao-`; usa apenas
`compose.recuperacao.yaml`; não publica portas; recusa projetos/volumes existentes.
Restaura em volumes separados e mostra contagens de materiais/fotos e histórico
Flyway. Em falha, os recursos isolados permanecem para diagnóstico; não há limpeza
automática de volumes. Não inicia backend, Flyway ou SMTP.

Para migrar, use a cópia restaurada e validada como origem do futuro deploy,
configure as raízes legadas e as permissões e teste downloads e moderação antes
de trocar o acesso público. Esse corte e a configuração do backend pertencem à
etapa de deploy; não execute o script contra volumes de produção.

## Rotina recomendada

Faça backup antes de atualizações e diariamente com uma janela de manutenção;
mantenha, por exemplo, 7 cópias diárias e 4 semanais, conforme a capacidade.
Copie para destino externo à VPS com criptografia, acesso restrito e alerta de falha.
Não inclua credenciais no pacote: guarde-as separadamente em um gerenciador seguro.
Repita o teste de restauração periodicamente e confira arquivos reais por download.
A automação externa e o armazenamento remoto serão configurados quando houver servidor.
