package br.com.srm.creditengine.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
public class SegurancaConfig {

    private static final String OPERADOR = "OPERADOR";
    private static final String TESOURARIA = "TESOURARIA";

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(acesso -> acesso
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/taxas/**").hasRole(TESOURARIA)
                        .requestMatchers(HttpMethod.GET, "/taxas/**").hasAnyRole(OPERADOR, TESOURARIA)
                        .requestMatchers(HttpMethod.GET, "/usuarios/eu").authenticated()
                        .anyRequest().hasRole(OPERADOR))
                .httpBasic(basic -> basic.authenticationEntryPoint((requisicao, resposta, erro) ->
                        escreverProblema(resposta, 401, "Unauthorized", "NAO_AUTENTICADO",
                                "Credenciais ausentes ou inválidas")))
                .exceptionHandling(excecoes -> excecoes.accessDeniedHandler((requisicao, resposta, erro) ->
                        escreverProblema(resposta, 403, "Forbidden", "ACESSO_NEGADO",
                                "Seu perfil não tem permissão para esta operação")));
        return http.build();
    }

    @Bean
    public UserDetailsService usuarios(PasswordEncoder codificador,
                                       @Value("${seguranca.senha.operador}") String senhaOperador,
                                       @Value("${seguranca.senha.tesouraria}") String senhaTesouraria) {
        return new InMemoryUserDetailsManager(
                User.withUsername("operador1").password(codificador.encode(senhaOperador)).roles(OPERADOR).build(),
                User.withUsername("operador2").password(codificador.encode(senhaOperador)).roles(OPERADOR).build(),
                User.withUsername("tesouraria1").password(codificador.encode(senhaTesouraria)).roles(TESOURARIA).build());
    }

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource configuracaoCors(@Value("${seguranca.cors.origem:http://localhost:3000}") String origem) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origem));
        config.setAllowedMethods(List.of("GET", "POST", "PUT"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }

    private static void escreverProblema(HttpServletResponse resposta, int status, String titulo,
                                         String codigo, String detalhe) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/problem+json");
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"codigo\":\"%s\"}"
                        .formatted(titulo, status, detalhe, codigo));
    }
}