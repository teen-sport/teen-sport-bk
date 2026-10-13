package com.uniquindio.teen_sport.security;

import static com.uniquindio.teen_sport.config.WebConfig.API_PREFIX;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
 *       exige un token válido.</li>
 *   <li>Las rutas exclusivas de un rol se restringen aquí, en el filtro, para rechazar al usuario
 *       sin permiso antes de leer o validar el cuerpo (de lo contrario recibiría un 400 con detalles
 *       de validación en lugar del 403). Los endpoints repiten la regla con {@code @PreAuthorize}
 *       ({@link EnableMethodSecurity}) como segunda barrera si una ruta cambia.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            API_PREFIX + "/auth/login",
            API_PREFIX + "/users/register"
    };

    /** Cuenta propia: cualquier rol autenticado. Debe evaluarse antes que {@link #ADMIN_ENDPOINTS}. */
    private static final String[] OWN_ACCOUNT_ENDPOINTS = {
            API_PREFIX + "/users/me",
            API_PREFIX + "/users/me/**"
    };

    /** Administración de cuentas y listados de apoyo (U6–U12). */
    private static final String[] ADMIN_ENDPOINTS = {
            API_PREFIX + "/users",
            API_PREFIX + "/users/**",
            API_PREFIX + "/coaches",
            API_PREFIX + "/guardians"
    };

    /** Catálogo de disciplinas (H1–H5): lectura para cualquier rol, escritura solo {@code ADMIN}. */
    private static final String[] DISCIPLINE_ENDPOINTS = {
            API_PREFIX + "/disciplines",
            API_PREFIX + "/disciplines/**"
    };

    /** Deportistas (E1–E4): administradores y acudientes; la propiedad se comprueba en el servicio. */
    private static final String[] ATHLETE_ENDPOINTS = {
            API_PREFIX + "/athletes",
            API_PREFIX + "/athletes/**"
    };

    /**
     * Disponibilidad y matrículas (E5–E8): administradores y acudientes; la propiedad se comprueba
     * en el servicio.
     */
    private static final String[] ENROLLMENT_ENDPOINTS = {
            API_PREFIX + "/availability",
            API_PREFIX + "/enrollments",
            API_PREFIX + "/enrollments/**"
    };

    /** Grupos (H6–H12): lectura para administradores y entrenadores; escritura solo {@code ADMIN}. */
    private static final String[] GROUP_ENDPOINTS = {
            API_PREFIX + "/groups",
            API_PREFIX + "/groups/**"
    };

    /** Franjas ya creadas (H13, H14): solo {@code ADMIN}. */
    private static final String[] SCHEDULE_ENDPOINTS = {
            API_PREFIX + "/schedules",
            API_PREFIX + "/schedules/**"
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
                        .requestMatchers(OWN_ACCOUNT_ENDPOINTS).authenticated()
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, DISCIPLINE_ENDPOINTS).authenticated()
                        .requestMatchers(DISCIPLINE_ENDPOINTS).hasRole("ADMIN")
                        .requestMatchers(ATHLETE_ENDPOINTS).hasAnyRole("ADMIN", "GUARDIAN")
                        .requestMatchers(ENROLLMENT_ENDPOINTS).hasAnyRole("ADMIN", "GUARDIAN")
                        .requestMatchers(HttpMethod.GET, GROUP_ENDPOINTS).hasAnyRole("ADMIN", "COACH")
                        .requestMatchers(GROUP_ENDPOINTS).hasRole("ADMIN")
                        .requestMatchers(SCHEDULE_ENDPOINTS).hasRole("ADMIN")
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
