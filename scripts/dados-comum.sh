#!/usr/bin/env bash
# Funções compartilhadas; requer Bash, Docker Compose v2, GNU tar e sha256sum.
set -euo pipefail

falhar() { printf '%s\n' "$*" >&2; exit 1; }

validar_arquivo_uploads() {
  local arquivo=$1 membro nome tipos
  # O armazenamento é plano: não aceita subpastas, links ou arquivos especiais.
  tipos=$(tar --quoting-style=escape -tvzf "$arquivo") || falhar 'Arquivo de uploads inválido.'
  while IFS= read -r membro; do
    case "${membro:0:1}" in -|d) ;; *) falhar 'Backup contém links ou arquivos especiais.' ;; esac
  done <<< "$tipos"
  while IFS= read -r membro; do
    case "$membro" in
      .|./) continue ;;
      ./*) nome=${membro#./} ;;
      *) falhar 'Backup contém caminho não permitido.' ;;
    esac
    [[ -n "$nome" && "$nome" != . && "$nome" != .. && "$nome" != */* && "$nome" != *\\* && "$nome" != *:* ]] \
      || falhar 'Backup contém caminho não permitido.'
  done < <(tar --quoting-style=escape -tzf "$arquivo")
}

validar_backup() {
  local pasta=$1
  [[ -f "$pasta/CONCLUIDO" && $(cat "$pasta/CONCLUIDO") == banco-academico-v1 ]] \
    || falhar 'Backup incompleto ou formato desconhecido.'
  [[ -s "$pasta/banco.dump" && -f "$pasta/uploads.tar.gz" && -f "$pasta/SHA256SUMS" ]] \
    || falhar 'Backup incompleto.'
  # Nomes fixos: não permite que um manifesto externo indique outros caminhos.
  [[ $(wc -l < "$pasta/SHA256SUMS") -eq 2 ]] || falhar 'Manifesto inválido.'
  grep -Eq '^[0-9a-f]{64} \*banco\.dump$' "$pasta/SHA256SUMS" || falhar 'Manifesto inválido.'
  grep -Eq '^[0-9a-f]{64} \*uploads\.tar\.gz$' "$pasta/SHA256SUMS" || falhar 'Manifesto inválido.'
  (cd "$pasta" && sha256sum --check --status SHA256SUMS) || falhar 'Checksum do backup não confere.'
  validar_arquivo_uploads "$pasta/uploads.tar.gz"
}

conferir_referencias() {
  local arquivo=$1 referencias=$2 nomes nome
  nomes=$(tar --quoting-style=escape -tzf "$arquivo")
  if [[ -n "$referencias" ]]; then
    while IFS= read -r nome; do
      [[ -n "$nome" && "$nome" != *\\* && "$nome" != */* && "$nome" != *:* ]] \
        || falhar 'Referência de material inválida; revise a migração.'
      grep -Fxq -- "./$nome" <<< "$nomes" || falhar 'Existe material no banco sem arquivo no backup.'
    done <<< "$referencias"
  fi
}
