package co.edu.uniquindio.proyecto.configuration;

import co.edu.uniquindio.proyecto.util.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración del canal WebSocket para habilitar la comunicación en tiempo real usando STOMP.
 * <p>
 * Esta clase define los endpoints para las conexiones WebSocket y configura el broker de mensajes,
 * permitiendo el uso de suscripciones (ej: <code>/topic</code>) y envíos de mensajes (ej: <code>/app</code>).
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    /**
     * Registra el endpoint STOMP accesible por los clientes WebSocket.
     * También aplica el interceptor de autenticación JWT durante el "handshake".
     *
     * @param registry Registro de endpoints STOMP.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)  // Interceptor que valida el token JWT al conectarse
                .setAllowedOriginPatterns("*")             // ⚠️ Considerar restringir en producción
                .withSockJS();                              // Soporte para navegadores antiguos con SockJS
    }

    /**
     * Configura el broker de mensajes interno y define los prefijos de destino para la aplicación.
     *
     * @param registry Registro del broker de mensajes.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");             // Destino de suscripciones (notificaciones push)
        registry.setApplicationDestinationPrefixes("/app"); // Prefijo para mensajes enviados desde el cliente
    }
}