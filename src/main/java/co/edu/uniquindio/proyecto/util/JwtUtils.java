package co.edu.uniquindio.proyecto.util;

import co.edu.uniquindio.proyecto.entity.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utilidad para la generación y validación de tokens JWT.
 * <p>
 * Esta clase se encarga de generar tokens JWT firmados, extrayendo los datos relevantes del usuario
 * (como username, userId y roles) y configurando la expiración. Además, proporciona métodos para parsear
 * y validar tokens.
 * </p>
 */
@Component
@Slf4j
public class JwtUtils {

    private static final long EXPIRATION_TIME = 3600000; // 1 hora en milisegundos

    /**
     * Genera un token JWT para el usuario proporcionado.
     *
     * @param user Usuario para el cual se genera el token.
     * @return Token JWT generado.
     */
    public String generateToken(User user) {
        log.debug("Generando token JWT para el usuario: {}", user.getUsername());
        TokenData tokenData = extractTokenData(user);
        Instant now = getCurrentInstant();
        Instant expiration = calculateExpiration(now);
        String token = buildJwtToken(tokenData, now, expiration);
        log.info("Token JWT generado para el usuario {} (expira a las {})", user.getUsername(), expiration);
        return token;
    }

    /**
     * Extrae los datos necesarios del usuario para la generación del token.
     *
     * @param user Usuario del cual extraer los datos.
     * @return Objeto {@code TokenData} con username, userId y roles.
     */
    private TokenData extractTokenData(User user) {
        return new TokenData(
                user.getUsername(),
                user.getId().toString(),
                extractRoles(user)
        );
    }

    /**
     * Extrae la lista de roles del usuario, filtrando aquellos que inician con "ROLE_".
     *
     * @param user Usuario del cual se extraen los roles.
     * @return Lista de roles como cadenas de texto.
     */
    private List<String> extractRoles(User user) {
        return Optional.ofNullable(user.getAuthorities())
                .orElse(Collections.emptyList())
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(role -> role.startsWith("ROLE_"))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el instante actual.
     *
     * @return Instant actual.
     */
    private Instant getCurrentInstant() {
        return Instant.now();
    }

    /**
     * Calcula la fecha de expiración del token basándose en el instante actual y el tiempo de expiración configurado.
     *
     * @param now Instante actual.
     * @return Instante en el que expira el token.
     */
    private Instant calculateExpiration(Instant now) {
        return now.plusMillis(EXPIRATION_TIME);
    }

    /**
     * Construye y firma el token JWT utilizando los datos proporcionados, el instante de emisión y expiración.
     *
     * @param tokenData Datos extraídos del usuario.
     * @param issuedAt  Instante en el que se emite el token.
     * @param expiration Instante en el que expira el token.
     * @return Token JWT firmado.
     */
    private String buildJwtToken(TokenData tokenData, Instant issuedAt, Instant expiration) {
        return Jwts.builder()
                .subject(tokenData.username())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .claim("roles", tokenData.roles())
                .claim("userId", tokenData.userId())
                .signWith(KeyUtils.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Parsea y verifica un token JWT.
     *
     * @param token Token JWT a parsear.
     * @return Objeto {@code Jws<Claims>} con los claims del token.
     * @throws io.jsonwebtoken.JwtException Si el token es inválido o ha expirado.
     */
    public Jws<Claims> parseToken(String token) {
        log.debug("Parseando token JWT");
        return Jwts.parser()
                .verifyWith(KeyUtils.getPublicKey())
                .build()
                .parseSignedClaims(token);
    }

    /**
     * Clase interna para agrupar los datos necesarios para la generación del token.
     *
     * @param username Nombre de usuario.
     * @param userId   Identificador del usuario.
     * @param roles    Lista de roles del usuario.
     */
    private record TokenData(String username, String userId, List<String> roles) {}
}
