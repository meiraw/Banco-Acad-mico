import test from 'node:test'
import assert from 'node:assert/strict'
import { getDocument } from 'pdfjs-dist/legacy/build/pdf.mjs'
import { createCanvas } from '@napi-rs/canvas'
import { opcoesPdf } from './pdf-config.js'

function duasPaginas() {
  const vermelho = '1 0 0 rg 10 10 80 80 re f\n'
  const azul = '0 0 1 rg 10 10 80 80 re f\n'
  const objetos = [
    '<< /Type /Catalog /Pages 2 0 R /OpenAction 7 0 R >>',
    '<< /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 100 100] /Resources <<>> /Contents 5 0 R >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 100 100] /Resources <<>> /Contents 6 0 R >>',
    `<< /Length ${vermelho.length} >>\nstream\n${vermelho}endstream`,
    `<< /Length ${azul.length} >>\nstream\n${azul}endstream`,
    '<< /Type /Action /S /JavaScript /JS (globalThis.__uploadPdfScriptExecuted = true;) >>',
  ]
  let texto = '%PDF-1.7\n'
  const offsets = [0]
  objetos.forEach((objeto, i) => { offsets.push(texto.length); texto += `${i + 1} 0 obj\n${objeto}\nendobj\n` })
  const xref = texto.length
  texto += `xref\n0 ${objetos.length + 1}\n0000000000 65535 f \n`
  offsets.slice(1).forEach(offset => { texto += `${String(offset).padStart(10, '0')} 00000 n \n` })
  texto += `trailer\n<< /Size ${objetos.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`
  return new Uint8Array(Buffer.from(texto))
}

test('prévia pinta as duas páginas sem executar ação JavaScript do PDF', async () => {
  delete globalThis.__uploadPdfScriptExecuted
  const tarefa = getDocument({ ...opcoesPdf(undefined), data: duasPaginas(), useWorkerFetch: false })
  try {
    const pdf = await tarefa.promise
    assert.equal(pdf.numPages, 2)
    for (const [numero, esperado] of [[1, [255, 0, 0, 255]], [2, [0, 0, 255, 255]]]) {
      const pagina = await pdf.getPage(numero)
      const canvas = createCanvas(100, 100)
      const contexto = canvas.getContext('2d')
      await pagina.render({ canvasContext: contexto, viewport: pagina.getViewport({ scale: 1 }) }).promise
      assert.deepEqual([...contexto.getImageData(50, 50, 1, 1).data], esperado)
    }
    assert.equal(globalThis.__uploadPdfScriptExecuted, undefined)
  } finally { await tarefa.destroy() }
})
