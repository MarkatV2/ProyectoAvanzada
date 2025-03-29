package co.edu.uniquindio.proyecto.configuration;

import co.edu.uniquindio.proyecto.exceptionhandler.auth.SecurityErrorHandler;
import co.edu.uniquindio.proyecto.util.KeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuración de seguridad de la aplicación.
 *
 * <p>Esta clase define las reglas de autorización y autenticación para la API, utilizando JWT para la autenticación,
 * configurando filtros de seguridad, políticas de sesión y CORS.</p>
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final SecurityErrorHandler securityErrorHandler;

    /**
     * Define la cadena de filtros de seguridad.
     *
     * @param http Objeto HttpSecurity a configurar.
     * @return SecurityFilterChain configurado.
     * @throws Exception Si ocurre algún error en la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configurando SecurityFilterChain");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(securityErrorHandler)
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(securityErrorHandler)
                );
        SecurityFilterChain chain = http.build();
        log.info("SecurityFilterChain configurado correctamente");
        return chain;
    }

    /**
     * Configura el convertidor de autenticación JWT, asignando las autoridades a partir de la reclamación "roles"
     * y utilizando "userId" como principal.
     *
     * @return JwtAuthenticationConverter configurado.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        log.debug("Configurando JwtAuthenticationConverter");
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        jwtConverter.setPrincipalClaimName("userId");

        return jwtConverter;
    }

    /**
     * Configura el filtro CORS para permitir solicitudes desde cualquier origen.
     *
     * @return CorsFilter configurado.
     */
    @Bean
    public CorsFilter corsFilter() {
        log.debug("Configurando CorsFilter");
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*"); // Permite todas las IPs.
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    /**
     * Provee un codificador de contraseñas utilizando BCrypt.
     *
     * @return PasswordEncoder configurado.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("Configurando BCryptPasswordEncoder");
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura el decodificador JWT utilizando la clave pública.
     *
     * @return JwtDecoder configurado.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.debug("Configurando JwtDecoder con clave pública");
        return NimbusJwtDecoder.withPublicKey(KeyUtils.getPublicKey()).build();
    }

    /**
     * Provee el AuthenticationManager basado en la configuración actual.
     *
     * @param config Configuración de autenticación.
     * @return AuthenticationManager.
     * @throws Exception Si ocurre algún error al obtener el AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        log.debug("Obteniendo AuthenticationManager desde la configuración");
        return config.getAuthenticationManager();
    }
}
