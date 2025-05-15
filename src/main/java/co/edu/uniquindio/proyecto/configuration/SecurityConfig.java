package co.edu.uniquindio.proyecto.configuration;

import co.edu.uniquindio.proyecto.exceptionhandler.auth.SecurityErrorHandler;
import co.edu.uniquindio.proyecto.util.JwtTokenFromCookieFilter;
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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final SecurityErrorHandler     securityErrorHandler;
    private final JwtTokenFromCookieFilter jwtTokenFromCookieFilter;

    /**
     * Configuración de CORS para que Spring Security la aplique al inicio del filter chain.
     */
   @Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Collections.singletonList("https://mariamarmolejo.github.io"));
    config.setAllowCredentials(true);
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setAllowedHeaders(Collections.singletonList("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}


    /**
     * Única cadena de seguridad: CORS, CSRF off, stateless, autorización, filtros y resource server.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configurando SecurityFilterChain");

        http
            // 1) Aplica CORS según corsConfigurationSource()
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 2) Deshabilita CSRF (API REST)
            .csrf(AbstractHttpConfigurer::disable)
            // 3) Stateless session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 4) Reglas de autorización
            .authorizeHttpRequests(auth -> auth
                // Permitimos preflight OPTIONS en todas las rutas
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Solo POST a /api/v1/users y /api/v1/users/ es público
                .requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/users/").permitAll()
                // Endpoints de auth también públicos
                .requestMatchers("/api/v1/auth/**").permitAll()
                // El resto requiere autenticación
                .anyRequest().authenticated()
            )
            // 5) Filtro que traslada el JWT desde cookie al header Authorization
            .addFilterBefore(jwtTokenFromCookieFilter, BearerTokenAuthenticationFilter.class)
            // 6) Resource Server JWT (solo en rutas autenticadas)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(securityErrorHandler)
            )
            // 7) Handler de accesos denegados
            .exceptionHandling(exceptions ->
                exceptions.accessDeniedHandler(securityErrorHandler)
            );

        log.info("SecurityFilterChain configurado correctamente");
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthorityPrefix("");
        gac.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(gac);
        converter.setPrincipalClaimName("userId");
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(KeyUtils.getPublicKey()).build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}