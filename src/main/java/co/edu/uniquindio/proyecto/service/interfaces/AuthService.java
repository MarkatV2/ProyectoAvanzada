package co.edu.uniquindio.proyecto.service.interfaces;


import co.edu.uniquindio.proyecto.dto.user.JwtAccessResponse;
import co.edu.uniquindio.proyecto.dto.user.JwtResponse;
import co.edu.uniquindio.proyecto.dto.user.LoginRequest;

/**
 * Servicio responsable de autenticar usuarios y generar JWT.
 */
public interface AuthService {

    /**
     * Autentica al usuario con las credenciales proporcionadas y genera un token JWT si son válidas.
     *
     * @param request objeto que contiene el email/usuario y contraseña.
     * @return respuesta que incluye el JWT y los datos del usuario autenticado.
     */
    JwtResponse authenticate(LoginRequest request);

    JwtAccessResponse refreshAccessToken(String refreshToken);
}

