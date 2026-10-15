package br.wm.banco.academico.Config;

import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.TokenService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class TokenServiceFilter extends OncePerRequestFilter {
    private final TokenService tokens;
    private final UsuarioRepository usuarios;
    public TokenServiceFilter(TokenService tokens, UsuarioRepository usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    throw new JWTVerificationException("Bearer necessario");
                }
                String email = tokens.validarToken(header.substring(7));
                var usuario = usuarios.findByEmail(email)
                        .orElseThrow(() -> new JWTVerificationException("Usuario inexistente"));
                tokens.validarVersao(header.substring(7), usuario);
                // O perfil atual do banco e a fonte de autoridade.
                var authentication = new UsernamePasswordAuthenticationToken(usuario.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (JWTVerificationException ex) {
                SecurityContextHolder.clearContext();
                SecurityResponse.write(response, 401);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
