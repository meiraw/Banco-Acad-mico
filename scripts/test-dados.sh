#!/usr/bin/env bash
# Testes com arquivos temporários e Docker simulado. Não acessa serviços reais.
set -euo pipefail
raiz=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$raiz/scripts/dados-comum.sh"
temporario=$(mktemp -d)
export DOCKER_TEST_LOG="$temporario/docker.log"
export DOCKER_TEST_UPLOADS="$temporario/origem"
mkdir "$DOCKER_TEST_UPLOADS" "$temporario/backups"
printf 'material fictício\n' > "$DOCKER_TEST_UPLOADS/material.pdf"
export RESTORE_POSTGRES_PASSWORD=senha-ficticia-exclusiva-de-teste

docker() {
  printf '%s\n' "$*" >> "$DOCKER_TEST_LOG"
  case "$*" in
    'ps -aq '*) printf '%s' "${PROJETO_EXISTENTE:-}" ;;
    'volume ls '*) ;;
    'volume inspect banco-restauracao-'*) [[ ${VOLUME_RESTAURACAO_EXISTENTE:-0} == 1 ]] ;;
    'volume inspect '*) [[ ${VOLUME_AUSENTE:-0} == 0 ]] ;;
    *'ps --services --status running') printf '%s' "${BACKEND_ATIVO:-}" ;;
    *'exec -T postgres pg_restore --list') cat > /dev/null ;;
    *'exec -T postgres sh -c '*pg_dump*) printf 'dump fictício\n' ;;
    *'exec -T postgres sh -c '*pg_restore*) cat > /dev/null ;;
    *'exec -T postgres sh -c '*--tuples-only*) cat > /dev/null; printf 'material.pdf\n' ;;
    *'exec -T postgres sh -c '*psql*) printf 'verificação simulada\n' ;;
    *'run --rm --no-deps -T arquivos -c '*'-czf -'*) tar -C "$DOCKER_TEST_UPLOADS" -czf - . ;;
    *'run --rm --no-deps -T arquivos -c '*'-xzf -'*) tar -tzf - > /dev/null ;;
    *'up -d --wait --wait-timeout 90 postgres') ;;
    *) printf 'Chamada Docker inesperada\n' >&2; return 1 ;;
  esac
}
export -f docker
esperar_falha() { if "$@" > /dev/null 2>&1; then falhar 'Uma operação insegura foi aceita.'; fi; }

bash "$raiz/scripts/backup.sh" --aplicacao-parada "$temporario/backups/local" "$DOCKER_TEST_UPLOADS" > /dev/null
validar_backup "$temporario/backups/local"
bash "$raiz/scripts/backup.sh" --aplicacao-parada "$temporario/backups/volume" --volume-docker > /dev/null
validar_backup "$temporario/backups/volume"
esperar_falha bash "$raiz/scripts/backup.sh" --aplicacao-parada "$temporario/backups/local" "$DOCKER_TEST_UPLOADS"
BACKEND_ATIVO=backend esperar_falha bash "$raiz/scripts/backup.sh" --aplicacao-parada "$temporario/backups/ativo" "$DOCKER_TEST_UPLOADS"
[[ ! -e "$temporario/backups/ativo" ]] || falhar 'Criou backup com backend ativo.'
esperar_falha bash "$raiz/scripts/backup.sh" --aplicacao-parada "$DOCKER_TEST_UPLOADS/dentro" "$DOCKER_TEST_UPLOADS"
[[ ! -e "$DOCKER_TEST_UPLOADS/dentro" ]] || falhar 'Criou destino dentro da origem.'
VOLUME_AUSENTE=1 esperar_falha bash "$raiz/scripts/backup.sh" --aplicacao-parada "$temporario/backups/ausente" --volume-docker
bash "$raiz/scripts/restaurar-teste.sh" "$temporario/backups/local" banco-restauracao-valido > /dev/null
esperar_falha bash "$raiz/scripts/restaurar-teste.sh" "$temporario/backups/local" banco-academico
PROJETO_EXISTENTE=ocupado esperar_falha bash "$raiz/scripts/restaurar-teste.sh" "$temporario/backups/local" banco-restauracao-ocupado
VOLUME_RESTAURACAO_EXISTENTE=1 esperar_falha bash "$raiz/scripts/restaurar-teste.sh" "$temporario/backups/local" banco-restauracao-volume-ocupado
printf 'alterado\n' >> "$temporario/backups/local/banco.dump"
esperar_falha bash "$raiz/scripts/restaurar-teste.sh" "$temporario/backups/local" banco-restauracao-corrompido
tar -C "$DOCKER_TEST_UPLOADS" --transform='s|^|../|' -czf "$temporario/traversal.tar.gz" material.pdf
esperar_falha bash -c 'source "$1/scripts/dados-comum.sh"; validar_arquivo_uploads "$2"' _ "$raiz" "$temporario/traversal.tar.gz"
esperar_falha bash -c 'source "$1/scripts/dados-comum.sh"; conferir_referencias "$2" ausente.pdf' _ "$raiz" "$temporario/backups/volume/uploads.tar.gz"
[[ $(cat "$DOCKER_TEST_UPLOADS/material.pdf") == 'material fictício' ]] || falhar 'Alterou arquivo de origem.'
printf '13 cenários de backup/restauração passaram com Docker simulado. Fixtures: %s\n' "$temporario"
