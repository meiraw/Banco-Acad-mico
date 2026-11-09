import { Link } from 'react-router-dom'
import { ArrowRight, BookOpen, Users, Star } from 'lucide-react'
import { Logo } from '../components/Logo.jsx'
import { ThemeToggle } from '../components/ThemeToggle.jsx'

const DESTAQUES = [
  { Icone: BookOpen, titulo: 'Organizado por semestre', texto: 'Materiais reunidos exatamente pela grade do curso.' },
  { Icone: Users, titulo: 'Feito pelos colegas', texto: 'Cada resumo, lista e slide vem de quem já cursou a disciplina.' },
  { Icone: Star, titulo: 'Fácil de encontrar', texto: 'Busca por título ou disciplina, com filtro por tipo de arquivo.' },
]

const ORIENTACOES = [
  ['Materiais de autoria própria', 'O estudante pode compartilhar conteúdos produzidos por ele, respeitando eventuais direitos de terceiros incorporados ao material.'],
  ['Materiais de terceiros', 'Livros, apostilas, artigos, slides e outros materiais produzidos por terceiros somente devem ser disponibilizados quando houver autorização, licença compatível ou outra hipótese legal aplicável.'],
  ['Identificação da autoria', 'O usuário deve identificar corretamente a autoria e a origem do conteúdo, quando aplicável.'],
  ['Publicação responsável', 'O usuário deve verificar as condições de uso e distribuição antes de enviar qualquer arquivo.'],
  ['Denúncias e remoção', 'Conteúdos com indícios de violação de direitos poderão ser analisados, restringidos ou removidos conforme as regras da plataforma e a legislação aplicável.'],
]

export function Home() {
  return (
    <div className="landing-page min-h-screen bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex min-h-20 max-w-6xl flex-wrap items-center gap-3 px-4 py-2 sm:gap-4">
          <Link to="/" className="mr-auto min-w-0">
            <Logo className="w-40 sm:w-52" />
          </Link>
          <nav aria-label="Navegação da página inicial" className="label-mono order-last flex w-full items-center justify-center gap-6 text-muted-foreground sm:order-none sm:ml-auto sm:w-auto">
            <a href="#sobre" className="hover:text-primary">Sobre</a>
            <a href="#termos" className="hover:text-primary">Termos</a>
          </nav>
          <ThemeToggle />
          <Link to="/auth" className="btn btn-primary">Entrar</Link>
        </div>
      </header>

      <main>
        <section className="mx-auto max-w-6xl px-4 py-20">
          <p className="label-mono text-secondary">Ciência da Computação · UNEMAT</p>
          <h1 className="mt-3 max-w-3xl text-4xl font-bold tracking-tight sm:text-6xl">
            O acervo de estudos feito pelos próprios acadêmicos
          </h1>
          <p className="mt-5 max-w-2xl text-lg text-muted-foreground">
            Resumos, listas resolvidas, slides e anotações organizados por semestre e disciplina.
            Um repositório de apoio — não substitui os sistemas oficiais da universidade.
          </p>
          <div className="mt-8">
            <Link to="/auth" className="btn btn-primary max-w-full whitespace-normal text-base px-6 py-3">
              Entrar com e-mail institucional <ArrowRight className="size-4" />
            </Link>
          </div>

          <div className="mt-16 grid gap-4 sm:grid-cols-3">
            {DESTAQUES.map(c => (
              <div key={c.titulo} className="card-surface p-6">
                <c.Icone className="size-6 text-primary" />
                <h2 className="mt-3 font-semibold">{c.titulo}</h2>
                <p className="mt-1 text-sm text-muted-foreground">{c.texto}</p>
              </div>
            ))}
          </div>
        </section>

        <section id="sobre" aria-labelledby="titulo-sobre" className="mx-auto max-w-6xl px-4 pb-16 sm:pb-20">
          <h2 id="titulo-sobre" className="text-2xl font-bold sm:text-3xl">Sobre o Banco Acadêmico</h2>
          <div className="mt-5 max-w-3xl space-y-4 leading-relaxed text-muted-foreground">
            <p>O Banco Acadêmico é uma plataforma web desenvolvida no contexto da disciplina de Engenharia de Software, voltada à organização, ao acesso e ao compartilhamento de conteúdos acadêmicos entre estudantes do curso de Ciência da Computação da Universidade do Estado de Mato Grosso (UNEMAT).</p>
            <p>A proposta é facilitar a localização de conteúdos relacionados às disciplinas, permitindo que estudantes compartilhem materiais de sua autoria, como resumos, anotações, listas de exercícios e outros recursos de apoio ao estudo.</p>
            <p>A plataforma possui finalidade acadêmica e colaborativa e não substitui os ambientes ou sistemas oficiais da universidade.</p>
          </div>
        </section>

        <section aria-labelledby="titulo-compartilhamento" className="mx-auto max-w-6xl px-4 pb-16 sm:pb-20">
          <div className="card-surface border-l-4 border-l-primary p-6 sm:p-8">
            <h2 id="titulo-compartilhamento" className="text-2xl font-bold">Compartilhe conhecimento com responsabilidade</h2>
            <div className="mt-4 max-w-3xl space-y-3 leading-relaxed text-muted-foreground">
              <p>O Banco Acadêmico incentiva a colaboração entre estudantes, respeitando os direitos de autoria e as condições de utilização dos conteúdos publicados.</p>
              <p>Antes de compartilhar um arquivo, o usuário deve verificar se possui autorização ou outra base legal que permita sua disponibilização.</p>
              <p>Materiais produzidos por terceiros, incluindo livros, apostilas, slides, artigos e outros documentos, podem estar protegidos por direitos autorais.</p>
              <p>O fato de um conteúdo ser utilizado em atividades educacionais não significa, por si só, que sua reprodução integral ou distribuição pública seja permitida.</p>
            </div>
          </div>
        </section>

        <section id="termos" aria-labelledby="titulo-termos" className="mx-auto max-w-6xl px-4 pb-16 sm:pb-20">
          <h2 id="titulo-termos" className="text-2xl font-bold sm:text-3xl">Direitos autorais e termos de publicação</h2>
          <p className="mt-4 max-w-3xl leading-relaxed text-muted-foreground">Este resumo reúne orientações informativas para publicação. Não substitui os Termos de Uso completos nem documentos jurídicos revisados.</p>
          <Link to="/termos" className="mt-3 inline-block text-sm font-semibold text-primary underline underline-offset-4">Ler Termos de Uso completos</Link>

          <h3 className="mt-8 text-xl font-semibold">Orientações de publicação</h3>
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {ORIENTACOES.map(([titulo, texto]) => (
              <article key={titulo} className="card-surface p-6">
                <h4 className="font-semibold">{titulo}</h4>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{texto}</p>
              </article>
            ))}
          </div>

          <h3 className="mt-10 text-xl font-semibold">Legislação brasileira</h3>
          <div className="mt-4 grid items-start gap-4 lg:grid-cols-3">
            <article className="card-surface p-6">
              <h4 className="font-semibold">Lei nº 9.610/1998 — Lei de Direitos Autorais</h4>
              <ul className="mt-3 list-disc space-y-2 pl-5 text-sm leading-relaxed text-muted-foreground">
                <li>Art. 7º: apresenta categorias de obras intelectuais protegidas.</li>
                <li>Art. 24: trata dos direitos morais do autor, como o reconhecimento da autoria e a integridade da obra.</li>
                <li>Arts. 28 e 29: tratam dos direitos patrimoniais e da autorização para utilização de obras.</li>
                <li>Art. 46: prevê limitações aos direitos autorais em situações específicas.</li>
              </ul>
              <p className="mt-3 text-sm leading-relaxed text-muted-foreground">Citar o autor não significa possuir autorização para reproduzir ou disponibilizar integralmente uma obra.</p>
              <a href="https://www.planalto.gov.br/ccivil_03/leis/l9610.htm" className="mt-4 inline-block text-sm font-semibold text-primary underline underline-offset-4">Consultar Lei de Direitos Autorais no Planalto</a>
            </article>
            <article className="card-surface p-6">
              <h4 className="font-semibold">Lei nº 12.965/2014 — Marco Civil da Internet</h4>
              <p className="mt-3 text-sm leading-relaxed text-muted-foreground">Estabelece princípios, garantias, direitos e deveres para o uso da internet no Brasil. O art. 31 prevê, até a entrada em vigor de lei específica, a aplicação da legislação autoral vigente à responsabilidade por infrações de direitos autorais e conexos de provedores de aplicações por conteúdo de terceiros.</p>
              <a href="https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2014/lei/l12965.htm" className="mt-4 inline-block text-sm font-semibold text-primary underline underline-offset-4">Consultar Marco Civil da Internet no Planalto</a>
            </article>
            <article className="card-surface p-6">
              <h4 className="font-semibold">Lei nº 13.709/2018 — LGPD</h4>
              <p className="mt-3 text-sm leading-relaxed text-muted-foreground">A Lei Geral de Proteção de Dados Pessoais estabelece regras para o tratamento de dados pessoais. O tratamento de dados dos usuários deverá observar essa legislação. Esta orientação não constitui uma Política de Privacidade completa.</p>
              <a href="https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm" className="mt-4 inline-block text-sm font-semibold text-primary underline underline-offset-4">Consultar LGPD no Planalto</a>
            </article>
          </div>

          <div className="mt-10 max-w-3xl">
            <h3 className="text-xl font-semibold">Responsabilidade pelos conteúdos publicados</h3>
            <p className="mt-4 leading-relaxed text-muted-foreground">O Banco Acadêmico disponibiliza recursos tecnológicos para organização e compartilhamento de conteúdos acadêmicos entre usuários. Cada usuário é responsável por observar os direitos de terceiros ao publicar materiais. A administração da plataforma poderá adotar medidas de análise, restrição ou remoção de conteúdos quando identificar possíveis irregularidades ou receber comunicações fundamentadas, observada a legislação aplicável.</p>
          </div>
        </section>
      </main>

      <footer className="border-t border-border">
        <div className="mx-auto flex max-w-6xl flex-wrap items-start justify-between gap-6 px-4 py-8 text-sm text-muted-foreground">
          <div>
            <p className="font-semibold text-foreground">© {new Date().getFullYear()} Banco Acadêmico</p>
            <p className="mt-1">Plataforma acadêmica colaborativa</p>
            <p className="mt-3">A plataforma não substitui os sistemas oficiais da UNEMAT.</p>
          </div>
          <nav aria-label="Navegação do rodapé" className="label-mono flex gap-4">
            <a href="#sobre" className="hover:text-primary">Sobre</a>
            <a href="#termos" className="hover:text-primary">Termos</a>
          </nav>
        </div>
      </footer>
    </div>
  )
}
