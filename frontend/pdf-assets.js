import { readFile, readdir } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'

const root = dirname(createRequire(import.meta.url).resolve('pdfjs-dist/package.json'))
const folders = ['cmaps', 'standard_fonts', 'wasm', 'iccs']

// Mantém nomes esperados pelo PDF.js e entrega todos os recursos localmente.
export function pdfAssets() {
  return {
    name: 'pdf-assets',
    configureServer(server) {
      server.middlewares.use('/pdfjs', async (req, res, next) => {
        const match = /^\/(cmaps|standard_fonts|wasm|iccs)\/([a-zA-Z0-9_.-]+)$/.exec((req.url || '').split('?')[0])
        if (!match || match[2].includes('..') || match[2].startsWith('quickjs')) return next()
        try {
          const bytes = await readFile(join(root, match[1], match[2]))
          res.setHeader('Content-Type', match[2].endsWith('.wasm') ? 'application/wasm' : match[2].endsWith('.js') ? 'text/javascript' : 'application/octet-stream')
          res.end(bytes)
        } catch { next() }
      })
    },
    async generateBundle() {
      for (const folder of folders) {
        for (const entry of await readdir(join(root, folder), { withFileTypes: true })) {
          if (!entry.isFile() || entry.name.startsWith('quickjs')) continue
          this.emitFile({ type: 'asset', fileName: `pdfjs/${folder}/${entry.name}`, source: await readFile(join(root, folder, entry.name)) })
        }
      }
    },
  }
}
