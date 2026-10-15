package br.wm.banco.academico.Service;

import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public TokenService(@Value("${security.jwt.secret}") String segredo) {
        if (segredo == null || segredo.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve possuir pelo menos 32 bytes");
        }
        this.algorithm = Algorithm.HMAC256(segredo);
        this.verifier = JWT.require(algorithm).withIssuer("banco-academico")
                .withClaimPresence("exp").withClaimPresence("sub").build();
    }

    public String validarToken(String token) {
        String email = verifier.verify(token).getSubject();
        if (email == null || email.isBlank()) {
            throw new com.auth0.jwt.exceptions.JWTVerificationException("Token sem usuario");
        }
        return email;
    }

    public String gerarToken(UsuarioModel usuario) {


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
