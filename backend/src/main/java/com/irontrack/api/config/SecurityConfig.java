package com.irontrack.api.config;

import com.irontrack.api.security.JwtAuthenticationFilter;
import com.irontrack.api.security.RestAccessDeniedHandler;
import com.irontrack.api.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança (07_ROADMAP_BACKEND.md §C.0 item 6 / §C.1):
 * sessão stateless, endpoints de autenticação e Swagger abertos
 * (03_CONTRATOS_API.md §1.3), demais endpoints exigindo o JWT de acesso
 * validado por {@link JwtAuthenticationFilter}, e os cabeçalhos HTTP de
 * segurança obrigatórios (05_DEVOPS_E_SEGURANCA.md §E.1). Falhas de
 * autenticação/autorização detectadas na cadeia de filtros (token
 * ausente/inválido) respondem com o payload de erro padronizado via
 * {@link RestAuthenticationEntryPoint}/{@link RestAccessDeniedHandler} —
 * nunca o {@code Http403ForbiddenEntryPoint} padrão do Spring Security, que
 * responderia sem corpo e sempre 403 mesmo para token ausente
 * (01_ARQUITETURA_E_PADROES.md §4.1 exige 401 nesse caso).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs/**",
            "/api-docs",
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                           RestAccessDeniedHandler restAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .headers(headers -> headers
                        // X-Content-Type-Options: nosniff
                        .contentTypeOptions(config -> { })
                        // X-Frame-Options: DENY
                        .frameOptions(config -> config.deny())
                        // Strict-Transport-Security: max-age=31536000; includeSubDomains
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        // Content-Security-Policy: default-src 'none' (API-only, nunca serve HTML)
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
