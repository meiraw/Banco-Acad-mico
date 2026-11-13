import { Link } from 'react-router-dom'
import { PaginaInformativa } from '../components/PaginaInformativa.jsx'

const ORIENTACOES = [
  ['Cadastro e acesso à conta', 'Na página de acesso, selecione Criar conta e informe nome, e-mail, semestre e senha. O curso utilizado pela plataforma é Ciência da Computação. Se esquecer a senha, use Esqueci minha senha, receba o código por e-mail, valide-o e defina uma nova senha. A entrega depende da configuração do serviço de e-mail.'],
  ['Perfil e senha', 'Em Configurações, use Editar perfil para atualizar o semestre ou enviar uma foto JPG/PNG de até 2 MB. Nome, curso e e-mail são somente para consulta nessa tela. Alterar senha exige a senha atual e encerra as sessões anteriores após salvar.'],
  ['Compartilhamento de materiais', 'No Início, selecione Compartilhar material. Preencha os campos, escolha a disciplina e o tipo da publicação, selecione um arquivo aceito e confirme sua responsabilidade pelo compartilhamento. Artigo exige referência bibliográfica; para Livro ela é opcional. Consulte os Termos de uso antes de publicar.'],
  ['Pesquisa e filtros', 'Use a barra de pesquisa no Início para localizar materiais por título ou disciplina. Clique no botão Filtros para abrir as opções de curso, semestre, disciplina e tipo de arquivo. Digitar na barra não abre os filtros automaticamente.'],
  ['Favoritos', 'Use o botão de favorito de um material disponível para adicioná-lo ou removê-lo. A lista da barra lateral é vinculada à sua conta e pode ser acessada em outros navegadores ou dispositivos após entrar. Materiais que deixarem de estar disponíveis não aparecem na lista.'],
  ['Fórum Acadêmico', 'Entre em Fórum pela barra lateral. Pesquise discussões, filtre por disciplina ou por suas próprias publicações e use Nova discussão para publicar uma dúvida. Abra uma discussão para responder. Você pode editar seu conteúdo; autores e administradores podem removê-lo conforme as permissões. A remoção do fórum é lógica.'],
  ['Moderação das publicações', 'Novos materiais ficam pendentes até a análise de um administrador. Estudantes acompanham o status em Minhas publicações, na barra lateral abaixo de Fórum. Materiais aprovados aparecem no repositório. Administradores mantêm o acesso pela página de Perfil e sua área de moderação para aprovar ou rejeitar materiais; uma notificação por e-mail não é necessária para que a fila possa ser consultada.'],
]

export function Ajuda() {
  return <PaginaInformativa titulo="Ajuda" descricao="Orientações para utilizar os recursos do Banco Acadêmico.">
    {ORIENTACOES.map(([titulo, texto]) => <section key={titulo} className="card-surface space-y-3 p-5 sm:p-6"><h2 className="text-lg font-semibold">{titulo}</h2><p className="text-sm leading-relaxed text-muted-foreground">{texto}</p></section>)}
    <p className="text-sm text-muted-foreground">Consulte também os <Link to="/termos" className="font-semibold text-primary underline">Termos de uso</Link> e as informações de <Link to="/privacidade" className="font-semibold text-primary underline">Privacidade</Link>.</p>
  </PaginaInformativa>
}
