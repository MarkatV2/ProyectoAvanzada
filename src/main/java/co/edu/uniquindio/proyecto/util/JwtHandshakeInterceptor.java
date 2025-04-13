package co.edu.uniquindio.proyecto.util;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

/**
 * Interceptor encargado de validar el token JWT antes de establecer la conexión WebSocket.
 * Extrae el ID de usuario del token y lo agrega a los atributos de la conexión.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    /**
     * Método ejecutado antes de la negociación del WebSocket para verificar y validar el token JWT.
     * Si el token es válido, extrae el "userId" y lo coloca en los atributos de la conexión WebSocket.
     *
     * @param request   La solicitud HTTP.
     * @param response  La respuesta HTTP.
     * @param wsHandler El manejador WebSocket.
     * @param attributes Atributos que se agregarán a la sesión de la conexión WebSocket.
     * @return true si el token es válido y el usuario es autenticado, false en caso contrario.
     */
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            // Extrae el token del parámetro de la URL
            String token = servletRequest.getServletRequest().getParameter("token");

            // Si no se encuentra el token, se retorna false
            if (token == null || token.isEmpty()) {
                log.warn("Token no proporcionado en la solicitud WebSocket");
                return false;
            }

            try {
                // Intenta parsear el token JWT
                Claims claims = jwtUtils.parseToken(token).getPayload();
                String userId = claims.get("userId", String.class);

                // Si el token es válido, agrega el userId a los atributos de la conexión WebSocket
                attributes.put("userId", userId);
                log.info("Conexión WebSocket autorizada para el usuario con ID: {}", userId);
                return true;
            } catch (Exception e) {
                // Si ocurre algún error durante el procesamiento del token, se rechaza la conexión
                log.error("Error al procesar el token JWT: {}", e.getMessage());
                return false;
            }
        }
        log.warn("Solicitud no válida para WebSocket (no es una instancia de ServletServerHttpRequest)");
        return false;
    }

    /**
     * Método ejecutado después de la negociación del WebSocket.
     * No se realiza ninguna acción adicional en este caso.
     *
     * @param request   La solicitud HTTP.
     * @param response  La respuesta HTTP.
     * @param wsHandler El manejador WebSocket.
     * @param exception Excepción que puede haber ocurrido durante la negociación.
     */
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // No es necesario realizar ninguna acción adicional después del handshake
        log.error("Error durante el handshake WebSocket: {}", exception.getMessage());
    }

}

