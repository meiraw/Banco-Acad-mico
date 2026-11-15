#!/usr/bin/env bash
set -euo pipefail
raiz=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$raiz/scripts/dados-comum.sh"

[[ $# -eq 3 && $1 == --aplicacao-parada ]] \
  || falhar 'Uso: bash scripts/backup.sh --aplicacao-parada DESTINO_NOVO PASTA_UPLOADS_OU_--volume-docker'
destino=$2
[[ ! -e "$destino" && ! -L "$destino" ]] || falhar 'O destino já existe; nenhum backup será sobrescrito.'
destino="$(cd -- "$(dirname -- "$destino")" && pwd -P)/$(basename -- "$destino")"
projeto=${COMPOSE_PROJECT_NAME:-banco-academico}
compose=(docker compose --project-directory "$raiz" -p "$projeto" -f "$raiz/compose.yaml")
ativos=$("${compose[@]}" ps --services --status running)
if grep -qx backend <<< "$ativos"; then falhar 'Pare o backend antes de copiar banco e arquivos.'; fi
origem=$3
if [[ "$origem" == --volume-docker ]]; then
  docker volume inspect "${projeto}_arquivos-academicos" > /dev/null \
    || falhar 'Volume de uploads não existe; informe a pasta local para o primeiro backup.'
  origem=''
else
  [[ -d "$origem" && ! -L "$origem" ]] || falhar 'Pasta local de uploads inválida.'
  origem=$(cd -- "$origem" && pwd -P)
fi
if [[ -n "$origem" && "$destino/" == "$origem/"* ]]; then
  falhar 'O destino não pode ficar dentro da pasta de uploads.'
fi
umask 077
mkdir -m 700 -- "$destino"
"${compose[@]}" exec -T postgres sh -c \
  'exec pg_dump --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --format=custom' > "$destino/banco.dump"
"${compose[@]}" exec -T postgres pg_restore --list < "$destino/banco.dump" > /dev/null
if [[ -n "$origem" ]]; then
  tar -C "$origem" -czf "$destino/uploads.tar.gz" -- .
else
  "${compose[@]}" run --rm --no-deps -T arquivos -c 'exec tar -C /data/uploads -czf - .' > "$destino/uploads.tar.gz"
fi
validar_arquivo_uploads "$destino/uploads.tar.gz"
referencias=$("${compose[@]}" exec -T postgres sh -c \
  'exec psql --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --tuples-only --no-align --set=ON_ERROR_STOP=1' \
  <<< "SELECT regexp_replace(replace(caminho_arquivo, chr(92), '/'), '^.*/', '') FROM materiais;")
conferir_referencias "$destino/uploads.tar.gz" "$referencias"
(cd "$destino" && sha256sum --binary banco.dump uploads.tar.gz > SHA256SUMS)
printf 'banco-academico-v1\n' > "$destino/CONCLUIDO"
validar_backup "$destino"
printf 'Backup concluído. Mantenha também uma cópia protegida fora do servidor.\n'
