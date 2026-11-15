import test from 'node:test'
import assert from 'node:assert/strict'
import { aprovacaoRecente, PRAZO_APROVACAO_RECENTE } from './moderacao.js'

const agora = Date.parse('2026-11-14T12:00:00Z')
const material = idade => ({ statusModeracao: 'APROVADO', decisaoEm: new Date(agora - idade).toISOString() })

test('aprovação aparece por quatro dias e desaparece no limite', () => {
  assert.equal(aprovacaoRecente(material(0), agora), true)
  assert.equal(aprovacaoRecente(material(PRAZO_APROVACAO_RECENTE - 1), agora), true)
  assert.equal(aprovacaoRecente(material(PRAZO_APROVACAO_RECENTE), agora), false)
  assert.equal(aprovacaoRecente(material(PRAZO_APROVACAO_RECENTE + 1), agora), false)
})

test('usa a aprovação mesmo se o envio for antigo; legado usa a data de publicação', () => {
  assert.equal(aprovacaoRecente({ ...material(1000), publicadoEm: '2020-01-01T00:00:00Z' }, agora), true)
  assert.equal(aprovacaoRecente({ statusModeracao: 'APROVADO', publicadoEm: new Date(agora).toISOString() }, agora), true)
})

test('não inventa aprovação recente para datas ausentes, inválidas, futuras ou outros status', () => {
  for (const dados of [{}, { decisaoEm: 'inválido' }, material(-1000), { ...material(0), statusModeracao: 'PENDENTE' }, { ...material(0), statusModeracao: 'REJEITADO' }]) {
    assert.equal(aprovacaoRecente({ statusModeracao: 'APROVADO', ...dados }, agora), false)
  }
})
