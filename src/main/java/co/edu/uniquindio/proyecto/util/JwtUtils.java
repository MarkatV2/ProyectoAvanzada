package co.edu.uniquindio.proyecto.util;

import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.InvalidRefreshTokenException;
import co.edu.uniquindio.proyecto.exception.RefreshTokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtException;
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
    // Tiempo de expiración del refresh token: 30 días (en milisegundos)
    private static final long REFRESH_EXPIRATION_TIME = 30L * 24 * 3600 * 1000; // 30 días

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
     * Genera un refresh token para el usuario proporcionado.
     * El refresh token tendrá una expiración de 30 días y contendrá únicamente el claim 'userId'.
     *
     * @param user Usuario para el cual se genera el refresh token.
     * @return Refresh token JWT generado.
     */
    public String generateRefreshToken(User user) {
        log.debug("Generando refresh token para el usuario: {}", user.getUsername());
        // En el refresh token se incluye sólo el 'userId' (además del subject que es el username)
        TokenData tokenData = new TokenData(user.getUsername(), user.getId().toString(), Collections.emptyList());
        Instant now = getCurrentInstant();
        Instant expiration = now.plusMillis(REFRESH_EXPIRATION_TIME);
        String token = buildJwtToken(tokenData, now, expiration);
        log.info("Refresh token generado para el usuario {} (expira a las {})", user.getUsername(), expiration);
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
     * Valida si un refresh token es válido. No retorna información del token,
     * solo verifica que esté bien firmado, no haya expirado y tenga un formato válido.
     *
     * @param token El refresh token a validar.
     * @throws RefreshTokenExpiredException si el token ha expirado.
     * @throws InvalidRefreshTokenException si el token es inválido.
     */
    public void validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(KeyUtils.getPublicKey())
                    .build()
                    .parseSignedClaims(token);

            log.debug("Refresh token válido.");
        } catch (ExpiredJwtException ex) {
            log.warn("Refresh token expirado.");
            throw new RefreshTokenExpiredException("El refresh token ha expirado", ex);
        } catch (JwtException ex) {
            log.warn("Refresh token inválido.");
            throw new InvalidRefreshTokenException("El refresh token es inválido", ex);
        }
    }

    /**
     * Extrae el userId de un refresh token JWT válido.
     *
     * @param refreshToken El refresh token JWT.
     * @return El ID del usuario como cadena.
     * @throws JwtException si el token es inválido o no puede parsearse.
     */
    public String extractUserId(String refreshToken) {
        Jws<Claims> jwsClaims = Jwts.parser()
                .verifyWith(KeyUtils.getPublicKey())
                .build()
                .parseSignedClaims(refreshToken);

        return jwsClaims.getPayload().get("userId", String.class);
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
