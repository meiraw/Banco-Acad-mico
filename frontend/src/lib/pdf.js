import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist'
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import { opcoesPdf } from './pdf-config.js'

GlobalWorkerOptions.workerSrc = workerUrl

// Somente pintura de páginas: sem camada de scripts, formulários, links ou anexos.
export const carregarPdf = url => getDocument(opcoesPdf(url, import.meta.env.BASE_URL))
