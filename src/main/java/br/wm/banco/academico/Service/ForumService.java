package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.*;
import br.wm.banco.academico.DTOs.Response.*;
import br.wm.banco.academico.Model.*;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ForumService {
    private final DiscussaoRepository discussoes;
    private final RespostaForumRepository respostas;
    private final UsuarioRepository usuarios;
    private final DisciplinasRepository disciplinas;

    public ForumService(DiscussaoRepository discussoes, RespostaForumRepository respostas,
                        UsuarioRepository usuarios, DisciplinasRepository disciplinas) {
        this.discussoes = discussoes; this.respostas = respostas;
        this.usuarios = usuarios; this.disciplinas = disciplinas;
    }
    private static ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
    private UsuarioModel usuario(String email) {
        return usuarios.findByEmail(email).orElseThrow(() -> erro(HttpStatus.UNAUTHORIZED, "Entre na sua conta."));
    }
    private DiscussaoModel discussao(UUID id, boolean bloquear) {
        return (bloquear ? discussoes.bloquear(id) : discussoes.findByIdAndExcluidoEmIsNull(id))
            .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Discussão não encontrada."));
    }
    private DisciplinasModel disciplina(UUID id) {
        if (id == null) throw erro(HttpStatus.BAD_REQUEST, "Selecione uma disciplina.");
        return disciplinas.findById(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Disciplina não encontrada."));
    }
    private static String texto(String valor, int limite, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().isBlank() || valor.length() > limite)
            throw erro(HttpStatus.BAD_REQUEST, campo + " obrigatório, com até " + limite + " caracteres.");
        String normalizado = valor.strip();
        if (normalizado.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)))
            throw erro(HttpStatus.BAD_REQUEST, campo + " não pode conter apenas espaços.");
        return normalizado;
    }
    private static PageRequest pagina(int numero, int tamanho, Sort.Direction ordem) {
        if (numero < 0 || numero > 10000 || tamanho < 1 || tamanho > 50)
            throw erro(HttpStatus.BAD_REQUEST, "Paginação inválida. Use até 50 itens por página e páginas de 0 a 10000.");
        return PageRequest.of(numero, tamanho, Sort.by(ordem, "criadoEm", "id"));
    }
    private static void autor(UsuarioModel dono, UsuarioModel atual, boolean moderacao) {
        boolean proprio = dono != null && dono.getId().equals(atual.getId());
        if (!proprio && !(moderacao && atual.getPerfil() == PerfilUsuario.ADMIN))
            throw erro(HttpStatus.FORBIDDEN, "Você não pode alterar este conteúdo.");
    }
    private static void versao(Long recebida, long atual) {
        if (recebida == null || recebida < 0) throw erro(HttpStatus.BAD_REQUEST, "Informe a versão do conteúdo.");
        if (recebida != atual) throw erro(HttpStatus.CONFLICT, "O conteúdo mudou. Atualize a página antes de editar.");
    }
    private DiscussaoResponseDTO dto(DiscussaoModel d) {
        var a = d.getAutor();
        return new DiscussaoResponseDTO(d.getId(), d.getTitulo(), d.getConteudo(), d.getDisciplina().getId(),
            d.getDisciplina().getNome(), a == null ? null : a.getId(), a == null ? "Conta excluída" : a.getNome(),
            d.getCriadoEm(), d.getAtualizadoEm(), respostas.countByDiscussaoIdAndExcluidoEmIsNull(d.getId()), d.getVersao());
    }
    private RespostaForumResponseDTO dto(RespostaForumModel r) {
        var a = r.getAutor();
        return new RespostaForumResponseDTO(r.getId(), r.getDiscussao().getId(), a == null ? null : a.getId(),
            a == null ? "Conta excluída" : a.getNome(), r.getConteudo(), r.getCriadoEm(), r.getAtualizadoEm(), r.getVersao());
    }
    public Page<DiscussaoResumoDTO> listar(String email, String q, UUID disciplinaId, boolean minhas,
                                         String ordem, int numero, int tamanho) {
        var atual = usuario(email);
        if (q == null || q.length() > 200) throw erro(HttpStatus.BAD_REQUEST, "Pesquisa deve ter até 200 caracteres.");
        if (!Set.of("recentes", "antigas").contains(ordem)) throw erro(HttpStatus.BAD_REQUEST, "Ordenação inválida.");
        var pagina = pagina(numero, tamanho, ordem.equals("recentes") ? Sort.Direction.DESC : Sort.Direction.ASC);
        String busca = "%" + q.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return discussoes.pesquisar(disciplinaId, minhas ? atual.getId() : null, busca, pagina);
    }
    public DiscussaoResponseDTO buscar(UUID id) { return dto(discussao(id, false)); }

    @Transactional
    public DiscussaoResponseDTO criar(String email, DiscussaoRequestDTO dados) {
        var d = new DiscussaoModel();
        d.setAutor(usuario(email)); d.setDisciplina(disciplina(dados.disciplinaId()));
        d.setTitulo(texto(dados.titulo(), 200, "Título"));
        d.setConteudo(texto(dados.conteudo(), 10000, "Conteúdo"));
        d.setCriadoEm(Instant.now()); d.setAtualizadoEm(d.getCriadoEm());
        return dto(discussoes.saveAndFlush(d));
    }
    @Transactional
    public DiscussaoResponseDTO editar(String email, UUID id, DiscussaoRequestDTO dados) {
        var atual = usuario(email); var d = discussao(id, true);
        autor(d.getAutor(), atual, false); versao(dados.versao(), d.getVersao());
        d.setTitulo(texto(dados.titulo(), 200, "Título")); d.setConteudo(texto(dados.conteudo(), 10000, "Conteúdo"));
        d.setDisciplina(disciplina(dados.disciplinaId())); d.setAtualizadoEm(Instant.now());
        discussoes.flush(); return dto(d);
    }
    @Transactional
    public void excluir(String email, UUID id) {
        var atual = usuario(email); var d = discussao(id, true);
        autor(d.getAutor(), atual, true); d.setExcluidoEm(Instant.now()); d.setAtualizadoEm(d.getExcluidoEm());
    }
    public Page<RespostaForumResponseDTO> respostas(UUID id, int numero, int tamanho) {
        var pagina = pagina(numero, tamanho, Sort.Direction.ASC);
        discussao(id, false); return respostas.listar(id, pagina);
    }
    @Transactional
    public RespostaForumResponseDTO responder(String email, UUID id, RespostaForumRequestDTO dados) {
        var atual = usuario(email); var d = discussao(id, true);
        var r = new RespostaForumModel(); r.setDiscussao(d); r.setAutor(atual);
        r.setConteudo(texto(dados.conteudo(), 5000, "Resposta"));
        r.setCriadoEm(Instant.now()); r.setAtualizadoEm(r.getCriadoEm());
        return dto(respostas.saveAndFlush(r));
    }
    private RespostaForumModel resposta(UUID discussaoId, UUID id) {
        return respostas.findByIdAndDiscussaoIdAndExcluidoEmIsNull(id, discussaoId)
            .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Resposta não encontrada nesta discussão."));
    }
    @Transactional
    public RespostaForumResponseDTO editarResposta(String email, UUID discussaoId, UUID id, RespostaForumRequestDTO dados) {
        var atual = usuario(email); discussao(discussaoId, true);
        var r = resposta(discussaoId, id); autor(r.getAutor(), atual, false); versao(dados.versao(), r.getVersao());
        r.setConteudo(texto(dados.conteudo(), 5000, "Resposta")); r.setAtualizadoEm(Instant.now());
        respostas.flush(); return dto(r);
    }
    @Transactional
    public void excluirResposta(String email, UUID discussaoId, UUID id) {
        var atual = usuario(email); discussao(discussaoId, true);
        var r = resposta(discussaoId, id); autor(r.getAutor(), atual, true);
        r.setExcluidoEm(Instant.now()); r.setAtualizadoEm(r.getExcluidoEm());
    }
}
