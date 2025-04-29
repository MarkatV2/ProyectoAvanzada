package co.edu.uniquindio.proyecto.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Filtro de Spring Security para obtener el token de acceso (access_token) desde las cookies y
 * agregarlo al header Authorization de la solicitud.
 *
 * <p>Este filtro asegura que el token JWT, si está presente en las cookies, se pase al encabezado
 * Authorization como un token Bearer.</p>
 */
@Slf4j
@Component
public class JwtTokenFromCookieFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token"; // Nombre de la cookie de acceso
    private static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION; // Nombre del header Authorization

    /**
     * Método que se ejecuta en cada solicitud HTTP, interceptando la solicitud antes de que llegue al controlador.
     *
     * @param request La solicitud HTTP.
     * @param response La respuesta HTTP.
     * @param filterChain La cadena de filtros para continuar con el procesamiento de la solicitud.
     * @throws ServletException Si ocurre un error en el procesamiento de la solicitud.
     * @throws IOException Si ocurre un error de entrada/salida.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> accessToken = getAccessTokenFromCookies(request);

        if (accessToken.isPresent()) {
            log.info("Accediendo a la solicitud con token de acceso desde la cookie");
            String bearerToken = accessToken.get();
            request = new RequestWrapper(request, bearerToken);
        } else {
            log.warn("No se encontró token de acceso en las cookies.");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        return ("/api/v1/users".equals(path) && "POST".equalsIgnoreCase(method)) ||
               path.startsWith("/api/v1/auth/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }

    /**
     * Método que obtiene el token de acceso desde las cookies de la solicitud.
     *
     * @param request La solicitud HTTP.
     * @return Un Optional que contiene el token de acceso si está presente, o un Optional vacío si no lo está.
     */
    private Optional<String> getAccessTokenFromCookies(HttpServletRequest request) {
        // Buscar la cookie de acceso
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
        }
        return Optional.empty();
    }

    /**
     * Este método no es necesario en este caso, ya que no necesitamos limpiar nada cuando el filtro se destruye.
     */
    @Override
    public void destroy() {
        // No es necesario destruir nada en este caso.
    }

    /**
     * Wrapper para la solicitud HTTP, utilizado para agregar el encabezado Authorization con el token JWT.
     */
    private static class RequestWrapper extends HttpServletRequestWrapper {

        private final String token;

        public RequestWrapper(HttpServletRequest request, String token) {
            super(request);
            this.token = token;
        }

        @Override
        public String getHeader(String name) {
            if (AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
                // Agregar el token JWT en el header Authorization
                return "Bearer " + token;
            }
            return super.getHeader(name);
        }
    }
}

