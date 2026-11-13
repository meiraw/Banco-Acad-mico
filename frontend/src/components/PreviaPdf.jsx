import { useEffect, useRef, useState } from 'react'

export function PreviaPdf({ url, titulo, miniatura = false }) {
  const area = useRef(null)
  const [visivel, setVisivel] = useState(false)
  const [resultado, setResultado] = useState(null)
  const [pagina, setPagina] = useState(1)
  const [erroPagina, setErroPagina] = useState('')
  const pdf = resultado?.url === url ? resultado.pdf : null
  const erro = (resultado?.url === url ? resultado.erro : '') || erroPagina

  useEffect(() => {
    const observer = new IntersectionObserver(([entrada]) => {
      if (entrada.isIntersecting) { setVisivel(true); observer.disconnect() }
    }, { rootMargin: '100px' })
    observer.observe(area.current)
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    if (!visivel || !url) return
    let ativo = true
    let tarefa
    setResultado(null)
    setPagina(1)
    import('../lib/pdf.js').then(({ carregarPdf }) => {
      if (!ativo) return
      tarefa = carregarPdf(url)
      return tarefa.promise.then(documento => {
        if (ativo) setResultado({ url, pdf: documento })
      })
    }).catch(() => {
      if (ativo) setResultado({ url, erro: 'Não foi possível gerar a prévia. Baixe o arquivo para abrir.' })
    })
    return () => { ativo = false; tarefa?.destroy().catch(() => {}) }
  }, [url, visivel])

  useEffect(() => {
    setErroPagina('')
    const container = area.current
    container.replaceChildren()
    if (!pdf) return
    let ativo = true
    let renderizacao
    pdf.getPage(pagina).then(folha => {
      if (!ativo) return
      const original = folha.getViewport({ scale: 1 })
      if (!Number.isFinite(original.width) || !Number.isFinite(original.height) || original.width <= 0 || original.height <= 0) throw new Error('Dimensões inválidas')
      const largura = miniatura ? 360 : 1000
      const escala = Math.min(largura / original.width, 8192 / Math.max(original.width, original.height), Math.sqrt(4_000_000 / (original.width * original.height)))
      const viewport = folha.getViewport({ scale: escala })
      const canvas = document.createElement('canvas')
      canvas.width = Math.max(1, Math.floor(viewport.width))
      canvas.height = Math.max(1, Math.floor(viewport.height))
      canvas.style.width = '100%'
      canvas.style.height = 'auto'
      canvas.setAttribute('role', 'img')
      canvas.setAttribute('aria-label', `${titulo}, página ${pagina}`)
      container.replaceChildren(canvas)
      renderizacao = folha.render({ canvasContext: canvas.getContext('2d'), viewport })
      return renderizacao.promise
    }).catch(() => {
      if (ativo) setErroPagina('Não foi possível gerar a prévia. Baixe o arquivo para abrir.')
    })
    return () => { ativo = false; renderizacao?.cancel(); container.replaceChildren() }
  }, [pdf, pagina, miniatura, titulo])

  return (
    <div className={miniatura ? 'w-full' : 'max-h-[60vh] overflow-auto'}>
      {erro && <p role="status" className="p-4 text-sm text-muted-foreground">{erro}</p>}
      {!pdf && !erro && <p role="status" className="p-4 text-sm text-muted-foreground">Carregando prévia…</p>}
      <div ref={area} className="min-h-1 w-full" />
      {pdf && !miniatura && <nav aria-label="Páginas do PDF" className="sticky bottom-0 flex items-center justify-center gap-3 bg-background p-2">
        <button type="button" className="btn btn-outline" disabled={pagina <= 1} onClick={() => setPagina(p => p - 1)}>Anterior</button>
        <span className="text-sm">{pagina} / {pdf.numPages}</span>
        <button type="button" className="btn btn-outline" disabled={pagina >= pdf.numPages} onClick={() => setPagina(p => p + 1)}>Próxima</button>
      </nav>}
    </div>
  )
}
