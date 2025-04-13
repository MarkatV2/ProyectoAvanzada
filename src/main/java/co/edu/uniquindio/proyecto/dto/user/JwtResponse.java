package co.edu.uniquindio.proyecto.dto.user;

/**
 * DTO utilizado para devolver el token JWT tras una autenticación exitosa.
 */
public record JwtResponse(
        String token
) {
}
