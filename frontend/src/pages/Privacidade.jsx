import { PaginaInformativa } from '../components/PaginaInformativa.jsx'

export function Privacidade() {
  return <PaginaInformativa titulo="Privacidade" descricao="Saiba como os recursos atuais do Banco Acadêmico utilizam seus dados.">
    <section className="card-surface space-y-3 p-5 sm:p-6"><h2 className="text-lg font-semibold">Dados da conta</h2>
      <p className="text-sm leading-relaxed text-muted-foreground">O cadastro registra nome, e-mail, curso e semestre informado. A senha é armazenada como hash BCrypt, e não como texto legível. A foto de perfil é opcional e fica armazenada no banco de dados.</p>
      <p className="text-sm leading-relaxed text-muted-foreground">Os dados da conta são utilizados no acesso à plataforma, na identificação das publicações e nas informações do perfil. Nas Configurações, nome, e-mail e curso são somente para consulta. Você pode atualizar o semestre, a foto e a senha pelos recursos disponíveis.</p>
    </section>
    <section className="card-surface space-y-3 p-5 sm:p-6"><h2 className="text-lg font-semibold">Materiais, favoritos e fórum</h2>
      <p className="text-sm leading-relaxed text-muted-foreground">Materiais, comentários, respostas e discussões podem identificar seus autores pelo nome. Materiais enviados passam por moderação: pendentes e rejeitados têm acesso restrito ao autor e à administração; os aprovados ficam disponíveis no repositório. O fórum exige autenticação.</p>
      <p className="text-sm leading-relaxed text-muted-foreground">Favoritos são associados à sua conta no banco e consultados por você. Não inclua dados pessoais sensíveis ou informações de terceiros nos arquivos e textos compartilhados.</p>
      <p className="text-sm leading-relaxed text-muted-foreground">A exclusão de discussões e respostas do fórum é lógica: elas deixam de aparecer, mas os registros permanecem armazenados. Isso não representa eliminação definitiva dos dados.</p>
    </section>
    <section className="card-surface space-y-3 p-5 sm:p-6"><h2 className="text-lg font-semibold">Navegador e mensagens de e-mail</h2>
      <p className="text-sm leading-relaxed text-muted-foreground">O navegador mantém o token e os dados locais da sessão em sessionStorage. A preferência de aparência fica em localStorage, separada por conta e navegador. O tema automático consulta a preferência de aparência do dispositivo.</p>
      <p className="text-sm leading-relaxed text-muted-foreground">A recuperação de senha usa o e-mail da conta. Quando configuradas, notificações de novos materiais encaminham informações da publicação à administração pelo serviço SMTP. O sistema também mantém controles de tentativas para proteger operações de acesso e envio.</p>
    </section>
    <section className="card-surface space-y-3 p-5 sm:p-6"><h2 className="text-lg font-semibold">Informações que dependem do responsável</h2>
      <p className="text-sm leading-relaxed text-muted-foreground">Esta página descreve o funcionamento implementado e não substitui uma política de privacidade completa. O responsável pela plataforma ainda precisa definir e divulgar sua identificação, canal de contato, bases legais, prazos de conservação e os procedimentos para solicitações sobre dados pessoais.</p>
      <p className="text-sm leading-relaxed text-muted-foreground">Hospedagem, provedores, backups e eventuais compartilhamentos dependem do ambiente de operação e não são definidos por esta página. Não há aqui promessa de prazo de exclusão nem atendimento automático a solicitações.</p>
      <a href="https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares" target="_blank" rel="noreferrer" className="inline-block text-sm font-semibold text-primary underline">Informações sobre direitos dos titulares na ANPD</a>
    </section>
  </PaginaInformativa>
}
