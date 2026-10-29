import test from 'node:test'
import assert from 'node:assert/strict'
import { CURSO_ACADEMICO, validarCadastro } from './cadastro.js'

const valido = { nome: 'Maria Silva', email: 'maria@gmail.com', semestreAtual: '1', senha: 'Senha123!', confirmacaoSenha: 'Senha123!' }

test('cadastro válido e curso fixo', () => {
  assert.equal(validarCadastro(valido), '')
  assert.equal(CURSO_ACADEMICO, 'Ciência da Computação')
  assert.equal(validarCadastro({ ...valido, semestreAtual: '8' }), '')
})
test('confirmação obrigatória e igual à senha', () => {
  assert.match(validarCadastro({ ...valido, confirmacaoSenha: '' }), /Confirme/)
  assert.match(validarCadastro({ ...valido, confirmacaoSenha: 'Outra123!' }), /igual/)
})
test('semestre obrigatório e inteiro de 1 a 8', () => {
  for (const semestreAtual of ['', '0', '9', '1.5']) assert.match(validarCadastro({ ...valido, semestreAtual }), /semestre/)
})
test('nome, e-mail e senha inválidos', () => {
  assert.match(validarCadastro({ ...valido, nome: ' ' }), /nome/)
  assert.match(validarCadastro({ ...valido, email: 'invalido' }), /e-mail/)
  assert.match(validarCadastro({ ...valido, senha: '1234567' }), /8 caracteres/)
  assert.match(validarCadastro({ ...valido, senha: 'á'.repeat(37) }), /72 bytes/)
})
