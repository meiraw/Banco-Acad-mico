package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.*;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

@Service
public class PerfilService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    public PerfilService(UsuarioRepository usuarios, PasswordEncoder encoder, JdbcTemplate jdbc) {
        this.usuarios = usuarios; this.encoder = encoder; this.jdbc = jdbc;
    }
    private UsuarioModel bloquear(String email) {
        var conta = usuarios.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return usuarios.bloquearPorId(conta.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private static void validar(boolean condicao, String mensagem) {
        if (!condicao) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
    @Transactional
    public UsuarioModel atualizar(String email, AtualizarPerfilRequestDTO dados) {
        validar(dados.semestreAtual() != null && dados.semestreAtual() >= 1 && dados.semestreAtual() <= 8,
            "Selecione um semestre entre 1 e 8.");
        var conta = bloquear(email);
        conta.setSemestreAtual(dados.semestreAtual());
        return usuarios.saveAndFlush(conta);
    }
    @Transactional
    public void alterarSenha(String email, AlterarSenhaRequestDTO dados) {
        String atual = dados.senhaAtual(), nova = dados.novaSenha();
        validar(atual != null && !atual.isBlank() && atual.getBytes(StandardCharsets.UTF_8).length <= 72, "Informe uma senha atual válida.");
        validar(nova != null && nova.codePoints().anyMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))
                && nova.length() >= 8 && nova.getBytes(StandardCharsets.UTF_8).length <= 72,
            "A nova senha deve ter pelo menos 8 caracteres e até 72 bytes UTF-8.");
        validar(nova.equals(dados.confirmacaoNovaSenha()), "A confirmação não corresponde à nova senha.");
        var conta = bloquear(email);
        validar(encoder.matches(atual, conta.getSenha()), "Senha atual incorreta.");
        conta.setSenha(encoder.encode(nova));
        conta.setVersaoCredenciais(conta.getVersaoCredenciais() + 1);
        usuarios.saveAndFlush(conta);
        jdbc.update("DELETE FROM recuperacoes_senha WHERE usuario_id = ?", conta.getId());
    }
}
