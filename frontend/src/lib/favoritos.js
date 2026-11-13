// Uma instância por sessão; os favoritos são consultados exclusivamente pela API.
export function criarFavoritos({ api, estaAtiva = () => true }) {
  let ativo = true
  let versao = 0
  let consulta = null
  let atualizarDepois = false
  const ouvintes = new Set()
  let estado = { ids: new Set(), pendentes: new Set(), pronto: false, carregando: false,
    erro: '', revisao: 0 }
  const atual = () => ativo && estaAtiva()
  const publicar = dados => {
    if (!atual()) return
    estado = { ...estado, ...dados }
    for (const ouvir of ouvintes) ouvir()
  }
  const exigirSessao = () => { if (!atual()) throw new Error('A sessão mudou. Entre novamente na sua conta.') }

  function carregar() {
    if (!atual()) return Promise.resolve()
    if (consulta) return consulta
    const inicio = versao
    publicar({ carregando: true, erro: '' })
    consulta = (async () => {
      const ids = new Set()
      let pagina = 0, paginas = 1
      while (pagina < paginas) {
        exigirSessao()
        const resposta = await api.favoritosIds(pagina)
        exigirSessao()
        for (const id of resposta.content) ids.add(id)
        paginas = resposta.totalPages
        pagina++
      }
      if (inicio !== versao) { atualizarDepois = true; return }
      const mudou = !estado.pronto || ids.size !== estado.ids.size || [...ids].some(id => !estado.ids.has(id))
      publicar({ ids, pronto: true, revisao: estado.revisao + (mudou ? 1 : 0) })
    })().catch(erro => publicar({ erro: erro.message })).finally(() => {
      consulta = null
      publicar({ carregando: false })
      if (atualizarDepois && atual()) { atualizarDepois = false; void carregar() }
    })
    return consulta
  }

  function confirmar(id, adicionar) {
    versao++
    const ids = new Set(estado.ids)
    adicionar ? ids.add(id) : ids.delete(id)
    publicar({ ids, revisao: estado.revisao + 1 })
  }

  async function alternar(id) {
    exigirSessao()
    if (!estado.pronto) throw new Error('Carregue seus favoritos antes de tentar novamente.')
    if (estado.pendentes.has(id)) return
    const adicionar = !estado.ids.has(id)
    publicar({ pendentes: new Set([...estado.pendentes, id]) })
    try {
      await (adicionar ? api.adicionarFavorito(id) : api.removerFavorito(id))
      exigirSessao()
      confirmar(id, adicionar)
    } finally {
      const pendentes = new Set(estado.pendentes); pendentes.delete(id)
      publicar({ pendentes })
    }
  }

  function invalidar(id) {
    if (id) confirmar(id, false)
    void carregar()
  }

  const aoFocar = () => { void carregar() }
  const aoVisivel = () => { if (globalThis.document?.visibilityState === 'visible') aoFocar() }
  function eventos(ligar) {
    const metodo = ligar ? 'addEventListener' : 'removeEventListener'
    globalThis.window?.[metodo]('focus', aoFocar)
    globalThis.document?.[metodo]('visibilitychange', aoVisivel)
  }
  return {
    snapshot: () => estado,
    assinar(ouvir) { if (!ouvintes.size) eventos(true); ouvintes.add(ouvir); return () => { ouvintes.delete(ouvir); if (!ouvintes.size) eventos(false) } },
    carregar, alternar, invalidar,
    descartar() { ativo = false; eventos(false); ouvintes.clear() },
  }
}
