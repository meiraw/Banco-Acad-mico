#!/usr/bin/env bash
set -euo pipefail
raiz=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$raiz/scripts/dados-comum.sh"
[[ $# -eq 2 ]] || falhar 'Uso: bash scripts/restaurar-teste.sh PASTA_BACKUP banco-restauracao-NOME_NOVO'
pasta=$(cd -- "$1" && pwd -P)
projeto=$2
[[ "$projeto" =~ ^banco-restauracao-[a-z0-9][a-z0-9-]*$ ]] || falhar 'Use um projeto novo com prefixo banco-restauracao-.'
: "${RESTORE_POSTGRES_PASSWORD:?Defina uma senha exclusiva para a restauração de teste}"
validar_backup "$pasta"
# Recusa reutilizar projeto ou volumes; jamais limpa/sobrescreve uma instalação.
[[ -z $(docker ps -aq --filter "label=com.docker.compose.project=$projeto") ]] || falhar 'Projeto já utilizado.'
[[ -z $(docker volume ls -q --filter "label=com.docker.compose.project=$projeto") ]] || falhar 'Volumes do projeto já existem.'
for volume in "${projeto}_dados-academicos" "${projeto}_arquivos-academicos"; do
  if docker volume inspect "$volume" > /dev/null 2>&1; then
    falhar 'Nome de volume já utilizado, mesmo sem rótulo Compose.'
  fi
done
compose=(docker compose --project-directory "$raiz" -p "$projeto" -f "$raiz/compose.recuperacao.yaml")
"${compose[@]}" up -d --wait --wait-timeout 90 postgres
"${compose[@]}" exec -T postgres sh -c \
  'exec pg_restore --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --no-owner --no-privileges --exit-on-error' < "$pasta/banco.dump"
"${compose[@]}" run --rm --no-deps -T arquivos -c \
  'test -z "$(ls -A /data/uploads)" && tar --no-same-owner -C /data/uploads -xzf -' < "$pasta/uploads.tar.gz"
referencias=$("${compose[@]}" exec -T postgres sh -c \
  'exec psql --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --tuples-only --no-align --set=ON_ERROR_STOP=1' \
  <<< "SELECT regexp_replace(replace(caminho_arquivo, chr(92), '/'), '^.*/', '') FROM materiais;")
conferir_referencias "$pasta/uploads.tar.gz" "$referencias"
"${compose[@]}" exec -T postgres sh -c \
  'exec psql --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --set=ON_ERROR_STOP=1 --command="SELECT count(*) AS materiais FROM materiais; SELECT count(*) AS fotos FROM fotos_perfil; SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;"'
printf 'Restauração isolada concluída no projeto %s. Nenhum serviço da aplicação foi iniciado.\n' "$projeto"
