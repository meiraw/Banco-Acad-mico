import test from 'node:test'
import assert from 'node:assert/strict'
import { buildSync } from 'esbuild'
import { createRequire } from 'node:module'
import { mkdtempSync, unlinkSync, rmdirSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

// Renderização HTML sem navegador: verifica navegação e semântica, não aparência visual.
const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const temporario = mkdtempSync(join(tmpdir(), 'ba-config-telas-'))
const saida = join(temporario, 'telas.cjs')
buildSync({
  stdin: { contents: `
    import React from 'react';
    import { renderToStaticMarkup } from 'react-dom/server';
    import { MemoryRouter } from 'react-router-dom';
    import { SessaoProvider } from './src/lib/sessao.jsx';
    import { TemaProvider } from './src/lib/useTema.js';
    import { Configuracoes } from './src/pages/Configuracoes.jsx';
    import { EditarPerfil } from './src/pages/EditarPerfil.jsx';
    import { AlterarSenha } from './src/pages/AlterarSenha.jsx';
    import { Privacidade } from './src/pages/Privacidade.jsx';
    import { Ajuda } from './src/pages/Ajuda.jsx';
    const telas = { Configuracoes, EditarPerfil, AlterarSenha, Privacidade, Ajuda };
    export function renderizar(nome, rota) {
      const Tela = telas[nome];
      return renderToStaticMarkup(<SessaoProvider><TemaProvider><MemoryRouter initialEntries={[rota]}><Tela /></MemoryRouter></TemaProvider></SessaoProvider>);
    }
  `, resolveDir: raiz, loader: 'jsx' },
  bundle: true, platform: 'node', format: 'cjs', jsx: 'automatic', outfile: saida,
  loader: { '.png': 'dataurl' }, logLevel: 'silent',
})
const { renderizar } = createRequire(import.meta.url)(saida)
test.after(() => { unlinkSync(saida); rmdirSync(temporario) })

function ambiente(t, autenticado = true, papel = 'USER') {
  const nomes = ['sessionStorage', 'localStorage', 'window']
  const originais = Object.fromEntries(nomes.map(n => [n, globalThis[n]]))
  const erroOriginal = console.error
  t.after(() => { for (const n of nomes) { if (originais[n] === undefined) delete globalThis[n]; else globalThis[n] = originais[n] } console.error = erroOriginal })
  console.error = (...args) => { if (!String(args[0]).includes('useLayoutEffect does nothing on the server')) erroOriginal(...args) }
  const dados = new Map()
  if (autenticado) {
    const token = `teste.${Buffer.from(JSON.stringify({ sub: 'tela@example.test', perfil: papel, exp: Math.floor(Date.now() / 1000) + 300 })).toString('base64url')}.teste`
    dados.set('banco-academico:token', token)
    dados.set('banco-academico:sessao', JSON.stringify({ email: 'tela@example.test', id: 'conta-teste', nome: 'Estudante <teste>', curso: 'Ciência da Computação', semestreAtual: 2 }))
  }
  globalThis.sessionStorage = { getItem: chave => dados.get(chave) ?? null }
  globalThis.localStorage = { getItem: () => null }
  globalThis.window = { matchMedia: () => ({ matches: false }) }
}

test('Configurações apresenta três cartões, cinco destinos e três temas sem botão Voltar', t => {
  ambiente(t)
  const html = renderizar('Configuracoes', '/configuracoes')
  assert.ok(html.indexOf('Conta e perfil') < html.indexOf('Aparência'))
  assert.ok(html.indexOf('Aparência') < html.indexOf('Informações e suporte'))
  for (const rota of ['/configuracoes/perfil', '/sobre', '/termos', '/privacidade', '/ajuda']) assert.ok(html.includes(`href="${rota}"`))
  assert.ok(!html.includes('href="/configuracoes/senha"'))
  assert.equal((html.match(/type="radio"/g) || []).length, 3)
  assert.ok(html.includes('value="automatico"')); assert.ok(!html.includes('> Voltar'))
  assert.match(html, /aria-current="page"[^>]*href="\/configuracoes"/)
  assert.ok(html.includes('Estudante &lt;teste&gt;'))
})

test('formulários preservam campos restritos e pedem senha atual e confirmação', t => {
  ambiente(t)
  const perfil = renderizar('EditarPerfil', '/configuracoes/perfil')
  for (const campo of ['perfil-nome', 'perfil-email', 'perfil-curso']) assert.ok(!perfil.includes(`id="${campo}"`))
  assert.ok(perfil.includes('<dl'))
  for (const informacao of ['Nome completo', 'E-mail', 'Curso', 'tela@example.test', 'Ciência da Computação']) assert.ok(perfil.includes(informacao))
  assert.ok(perfil.includes('id="perfil-semestre"'))
  assert.ok(perfil.includes('type="file"')); assert.ok(perfil.includes('image/jpeg,image/png'))
  const senha = renderizar('AlterarSenha', '/configuracoes/senha')
  for (const campo of ['senha-atual', 'nova-senha-conta', 'confirmacao-senha-conta']) assert.ok(senha.includes(`id="${campo}"`))
  assert.equal((senha.match(/type="password"/g) || []).length, 3)
  assert.ok(senha.includes('outras sessões da conta também serão encerradas'))
})

test('Privacidade e Ajuda são acessíveis como visitante sem inventar atendimento', t => {
  ambiente(t, false)
  const privacidade = renderizar('Privacidade', '/privacidade')
  assert.ok(privacidade.includes('não substitui uma política de privacidade completa'))
  assert.ok(privacidade.includes('prazos de conservação'))
  assert.ok(!privacidade.includes('Navegação principal'))
  const ajuda = renderizar('Ajuda', '/ajuda')
  for (const titulo of ['Cadastro e acesso', 'Pesquisa e filtros', 'Favoritos', 'Fórum Acadêmico', 'Moderação']) assert.ok(ajuda.includes(titulo))
  assert.ok(ajuda.includes('href="/termos"'))
})

test('estudante acessa Minhas publicações abaixo do Fórum e identificação não abre Perfil', t => {
  ambiente(t)
  const html = renderizar('Configuracoes', '/configuracoes')
  assert.ok(!html.includes('href="/perfil"'))
  assert.ok(html.includes('href="/configuracoes/perfil"'))
  const forum = html.indexOf('href="/forum"'), publicacoes = html.indexOf('href="/minhas-publicacoes"'), favoritos = html.indexOf('href="/favoritos"')
  assert.ok(forum >= 0 && forum < publicacoes && publicacoes < favoritos)
})

test('administrador mantém acesso ao Perfil e não recebe o novo item do estudante', t => {
  ambiente(t, true, 'ADMIN')
  const html = renderizar('Configuracoes', '/configuracoes')
  assert.ok(html.includes('href="/perfil"'))
  assert.ok(!html.includes('href="/minhas-publicacoes"'))
})
