package br.wm.banco.academico.Config;

import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokens,
            UsuarioRepository usuarios, UrlBasedCorsConfigurationSource corsSource) throws Exception {
        return http
                // Bearer no header; sem autenticacao por cookies de sessao.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> SecurityResponse.write(res, 401))
                        .accessDeniedHandler((req, res, ex) -> SecurityResponse.write(res, 403)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/esqueci-senha", "/auth/redefinir-senha", "/usuario").permitAll()
                        .requestMatchers("/usuario", "/usuario/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/curso", "/curso/**", "/semestres", "/semestres/**",
                                "/disciplinas", "/disciplinas/**", "/material", "/material/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/material").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/curso", "/curso/**", "/semestres", "/semestres/**",
                                "/disciplinas", "/disciplinas/**", "/material", "/material/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .addFilterBefore(new TokenServiceFilter(tokens, usuarios), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsSource(
            @Value("${security.cors.allowed-origins:http://localhost:5173}") List<String> origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
