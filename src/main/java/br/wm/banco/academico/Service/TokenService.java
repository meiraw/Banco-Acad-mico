package br.wm.banco.academico.Service;

import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {
    private final String segredo = "banco-academico-chave-secreta";

    public String gerarToken(UsuarioModel usuario) {

        Algorithm algorithm = Algorithm.HMAC256(segredo);

        return JWT.create()
                .withIssuer("banco-academico")
                .withSubject(usuario.getEmail())
                .withClaim("perfil", usuario.getPerfil().name())
                .withExpiresAt(
                        Instant.now().plus(2, ChronoUnit.HOURS)
                )
                .sign(algorithm);
    }
}
