export function PaginacaoForum({ pagina, onChange, desabilitado = false }) {
  if (!pagina || pagina.totalPages < 2) return null
  return <nav aria-label="Paginação" className="flex flex-wrap items-center justify-between gap-3">
    <button type="button" className="btn btn-outline" disabled={desabilitado || pagina.first} onClick={() => onChange(pagina.number - 1)}>Anterior</button>
    <span className="text-sm text-muted-foreground">Página {pagina.number + 1} de {pagina.totalPages}</span>
    <button type="button" className="btn btn-outline" disabled={desabilitado || pagina.last} onClick={() => onChange(pagina.number + 1)}>Próxima</button>
  </nav>
}
