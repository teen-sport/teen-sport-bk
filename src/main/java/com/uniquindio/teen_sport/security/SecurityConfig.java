package com.uniquindio.teen_sport.security;

import static com.uniquindio.teen_sport.config.WebConfig.API_PREFIX;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cadena de seguridad HTTP de la API.
 *
 * <ul>
 *   <li>Sin sesión: cada solicitud se autentica con su JWT Bearer.</li>
 *   <li>CSRF desactivado: la credencial viaja en la cabecera {@code Authorization}, que el
 *       navegador nunca envía de forma automática, y la API no usa cookies; por eso un sitio
 *       ajeno no puede forjar solicitudes autenticadas.</li>
 *   <li>CORS limitado a los orígenes de {@link CorsProperties}.</li>
 *   <li>Públicos: login, registro de acudientes, salud y documentación OpenAPI. Todo lo demás
 *       exige un token válido; los roles de cada operación se comprueban además en su endpoint.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            API_PREFIX + "/auth/login",
            API_PREFIX + "/users/register"
    };

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health", "/actuator/health/**",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
            "/error"
    };

    /**
     * <p>CORS toma el bean {@code corsConfigurationSource} por nombre: inyectarlo por tipo es
     * ambiguo porque Spring MVC registra otro {@link CorsConfigurationSource}.</p>
     *
     * @param http         configurador de Spring Security
     * @param errorHandler respuestas 401/403 con el formato del contrato
     * @return cadena de filtros de la API
     * @throws Exception si la configuración es inválida
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
        return http.build();
    }

    /**
     * @return codificador BCrypt de contraseñas (exigido por el contrato)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * @param properties orígenes autorizados
     * @return política CORS de la API
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Convierte el claim {@code role} en la autoridad {@code ROLE_<rol>} que usan las reglas de acceso.
     */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenClaims.ROLE);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
