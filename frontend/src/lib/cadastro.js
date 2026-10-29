import { isEmailValido } from './academico.js'

export const CURSO_ACADEMICO = 'Ciência da Computação'

export function validarCadastro({ nome, email, semestreAtual, senha, confirmacaoSenha }) {
  if (!nome.trim()) return 'Informe seu nome completo.'
  if (nome.trim().length > 200) return 'O nome deve ter no máximo 200 caracteres.'
  if (!isEmailValido(email)) return 'Informe um e-mail válido, como nome@gmail.com.'
  if (email.trim().length > 200) return 'O e-mail deve ter no máximo 200 caracteres.'
  if (!/^[1-8]$/.test(String(semestreAtual))) return 'Selecione seu semestre atual, do 1º ao 8º.'
  if (!senha.trim() || senha.length < 8) return 'A senha precisa ter pelo menos 8 caracteres.'
  if (new TextEncoder().encode(senha).length > 72) return 'A senha pode ter no máximo 72 bytes.'
  if (!confirmacaoSenha) return 'Confirme sua senha.'
  if (senha !== confirmacaoSenha) return 'A confirmação de senha deve ser igual à senha.'
  return ''
}
