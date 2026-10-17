package com.uniquindio.teen_sport.security;

import static com.uniquindio.teen_sport.config.WebConfig.API_PREFIX;

import com.uniquindio.teen_sport.entity.RoleName;
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

    private static final String ADMIN = RoleName.ADMIN.name();
    private static final String COACH = RoleName.COACH.name();
    private static final String GUARDIAN = RoleName.GUARDIAN.name();

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
     * Revisión de matrículas (E12, E13, E15): solo {@code ADMIN}. Debe evaluarse antes que
     * {@link #ENROLLMENT_ENDPOINTS}.
     */
    private static final String[] ENROLLMENT_REVIEW_ENDPOINTS = {
            API_PREFIX + "/enrollments/*/confirm",
            API_PREFIX + "/enrollments/*/reject",
            API_PREFIX + "/enrollments/*/group"
    };

    /**
     * Registro de pagos y marca de mora (E17, E18): solo {@code ADMIN}. Debe evaluarse antes que
     * {@link #ENROLLMENT_ENDPOINTS}.
     */
    private static final String[] PAYMENT_ADMIN_ENDPOINTS = {
            API_PREFIX + "/enrollments/*/payments",
            API_PREFIX + "/enrollments/*/payment-status"
    };

    /**
     * Reporte de pago (E16): solo {@code GUARDIAN}, sobre sus matrículas. Debe evaluarse antes que
     * {@link #ENROLLMENT_ENDPOINTS}.
     */
    private static final String PAYMENT_REPORT_ENDPOINT = API_PREFIX + "/enrollments/*/payment-report";

    /** Historial de pagos (E19): administradores y acudientes; la propiedad se aplica en el servicio. */
    private static final String PAYMENT_HISTORY_ENDPOINT = API_PREFIX + "/payments";

    /**
     * Disponibilidad y matrículas (E5–E8, E14): administradores y acudientes; la propiedad se comprueba
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

    /**
     * Lista y registro de asistencia (A7, A8): consulta {@code ADMIN} y {@code COACH}; registro solo
     * {@code COACH}. Debe evaluarse antes que {@link #SESSION_ENDPOINTS}.
     */
    private static final String SESSION_ATTENDANCE_ENDPOINT = API_PREFIX + "/sessions/*/attendance";

    /**
     * Sesiones (A1–A6): lectura para los tres roles; programación para {@code ADMIN} y {@code COACH}. El
     * alcance de cada rol se aplica en el servicio.
     */
    private static final String[] SESSION_ENDPOINTS = {
            API_PREFIX + "/sessions",
            API_PREFIX + "/sessions/**"
    };

    /** Historial de asistencia (A9): los tres roles; el alcance se aplica en el servicio. */
    private static final String ATTENDANCE_HISTORY_ENDPOINT = API_PREFIX + "/attendance-records";

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
                        .       requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(OWN_ACCOUNT_ENDPOINTS).authenticated()
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, DISCIPLINE_ENDPOINTS).authenticated()
                        .requestMatchers(DISCIPLINE_ENDPOINTS).hasRole(ADMIN)
                        .requestMatchers(ATHLETE_ENDPOINTS).hasAnyRole(ADMIN, GUARDIAN)
                        .requestMatchers(ENROLLMENT_REVIEW_ENDPOINTS).hasRole(ADMIN)
                        .requestMatchers(PAYMENT_ADMIN_ENDPOINTS).hasRole(ADMIN)
                        .requestMatchers(PAYMENT_REPORT_ENDPOINT).hasRole(GUARDIAN)
                        .requestMatchers(PAYMENT_HISTORY_ENDPOINT).hasAnyRole(ADMIN, GUARDIAN)
                        .requestMatchers(ENROLLMENT_ENDPOINTS).hasAnyRole(ADMIN, GUARDIAN)
                        .requestMatchers(HttpMethod.GET, GROUP_ENDPOINTS).hasAnyRole(ADMIN, COACH)
                        .requestMatchers(GROUP_ENDPOINTS).hasRole(ADMIN)
                        .requestMatchers(SCHEDULE_ENDPOINTS).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, SESSION_ATTENDANCE_ENDPOINT).hasAnyRole(ADMIN, COACH)
                        .requestMatchers(SESSION_ATTENDANCE_ENDPOINT).hasRole(COACH)
                        .requestMatchers(HttpMethod.GET, SESSION_ENDPOINTS).hasAnyRole(ADMIN, COACH, GUARDIAN)
                        .requestMatchers(SESSION_ENDPOINTS).hasAnyRole(ADMIN, COACH)
                        .requestMatchers(ATTENDANCE_HISTORY_ENDPOINT).hasAnyRole(ADMIN, COACH, GUARDIAN)
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
