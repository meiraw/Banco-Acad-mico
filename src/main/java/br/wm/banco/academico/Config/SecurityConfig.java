package br.wm.banco.academico.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
    public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

            http
                    .csrf(csrf -> csrf.disable()) // desativa a proteção CSRF nesse estágio da nossa API, o que evita que nossos POST, PUT e DELETE sejam bloqueados por ela durante esses testes.
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll() // Por enquanto, permita acessar qualquer endpoint sem autenticação.
                    );

            return http.build();
        }
}
