package br.wm.banco.academico.Service;

import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class FotoPerfilService {
    private static final long LIMITE = 2 * 1024 * 1024;
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;

    public FotoPerfilService(UsuarioRepository usuarios, JdbcTemplate jdbc) {
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    public UsuarioModel usuarioAtual(String email) {
        return usuarios.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre novamente na sua conta."));
    }

    public boolean possuiFoto(UUID usuarioId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM fotos_perfil WHERE usuario_id = ?)", Boolean.class, usuarioId));
    }

    public byte[] buscar(String email) {
        var fotos = jdbc.query("SELECT conteudo FROM fotos_perfil WHERE usuario_id = ?",
                (rs, n) -> rs.getBytes(1), usuarioAtual(email).getId());
        if (fotos.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sua conta ainda não tem foto.");
        return fotos.getFirst();
    }

    @Transactional
    public void salvar(String email, MultipartFile arquivo) {
        byte[] foto = validarImagem(arquivo);
        // Bloqueio serializa trocas simultâneas da mesma conta.
        UsuarioModel conta = usuarioAtual(email);
        usuarios.bloquearPorId(conta.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
        int atualizados = jdbc.update("UPDATE fotos_perfil SET conteudo = ? WHERE usuario_id = ?", foto, conta.getId());
        if (atualizados == 0) jdbc.update("INSERT INTO fotos_perfil(usuario_id, conteudo) VALUES (?, ?)", conta.getId(), foto);
    }

    private byte[] validarImagem(MultipartFile arquivo) {
        if (arquivo.isEmpty()) throw invalida("Selecione uma foto.");
        if (arquivo.getSize() > LIMITE) throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter no máximo 2 MB.");
        try (var source = arquivo.getInputStream(); var input = ImageIO.createImageInputStream(source)) {
            if (input == null) throw invalida("Não foi possível ler a foto.");
            var leitores = ImageIO.getImageReaders(input);
            if (!leitores.hasNext()) throw invalida("Use uma imagem JPG ou PNG válida.");
            var leitor = leitores.next();
            try {
                if (!Set.of("JPEG", "JPG", "PNG").contains(leitor.getFormatName().toUpperCase(java.util.Locale.ROOT))) {
                    throw invalida("Use uma imagem JPG ou PNG.");
                }
                leitor.setInput(input);
                int largura = leitor.getWidth(0), altura = leitor.getHeight(0);
                if (largura <= 0 || altura <= 0 || (long) largura * altura > 16_000_000) {
                    throw invalida("A imagem deve ter no máximo 16 megapixels.");
                }
                BufferedImage original = leitor.read(0);
                double escala = Math.min(1.0, 512.0 / Math.max(largura, altura));
                BufferedImage reduzida = new BufferedImage(Math.max(1, (int) (largura * escala)),
                        Math.max(1, (int) (altura * escala)), BufferedImage.TYPE_INT_ARGB);
                var desenho = reduzida.createGraphics();
                try {
                    desenho.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    desenho.drawImage(original, 0, 0, reduzida.getWidth(), reduzida.getHeight(), null);
                } finally { desenho.dispose(); }
                var output = new ByteArrayOutputStream();
                ImageIO.write(reduzida, "png", output);
                return output.toByteArray();
            } finally { leitor.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw invalida("Não foi possível ler a foto. Use uma imagem JPG ou PNG válida.");
        }
    }

    private static ResponseStatusException invalida(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
