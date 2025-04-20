package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.user.JwtAccessResponse;
import co.edu.uniquindio.proyecto.dto.user.JwtResponse;
import co.edu.uniquindio.proyecto.dto.user.LoginRequest;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.auth.AccountDisabledException;
import co.edu.uniquindio.proyecto.exception.user.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.interfaces.AuthService;
import co.edu.uniquindio.proyecto.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de la autenticación de usuarios y generación de tokens JWT.
 *
 * <p>Este servicio implementa la lógica para validar las credenciales proporcionadas,
 * autenticar al usuario mediante el `AuthenticationManager` de Spring Security,
 * y generar un token JWT que se usará para acceder a recursos protegidos.</p>
 *
 * <p>Además, gestiona los posibles errores durante la autenticación, lanzando excepciones personalizadas
 * que pueden ser manejadas globalmente por controladores de excepciones (handlers).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImplements userDetailsService;

    /**
     * Autentica a un usuario y genera un token JWT si la autenticación es exitosa.
     *
     * @param request Objeto que contiene las credenciales del usuario.
     * @return Un objeto {@code JwtResponse} con el token JWT.
     * @throws InvalidPasswordException      Si las credenciales son incorrectas.
     * @throws AccountDisabledException      Si la cuenta del usuario está deshabilitada.
     * @throws UserNotFoundException         Si el usuario no es encontrado.
     * @throws AuthenticationServiceException Para otros errores inesperados durante la autenticación.
     */
    @Override
    public JwtResponse authenticate(LoginRequest request) {
        log.info("Inicio de autenticación para el usuario: {}", request.userName());

        validateRequest(request);

        try {
            // Cargar detalles del usuario
            UserDetails userDetails = loadUserDetails(request.userName());

            // Realizar la autenticación
            Authentication authentication = performAuthentication(request, userDetails);

            // Extraer usuario y verificar estado
            User user = (User) authentication.getPrincipal();

            // Generar y retornar el token
            JwtResponse tokenResponse = generateJwtToken(user);
            log.info("Usuario '{}' autenticado exitosamente", user.getUsername());
            return tokenResponse;

        } catch (BadCredentialsException ex) {
            log.warn("Credenciales incorrectas para el usuario '{}'", request.userName(), ex);
            throw new InvalidPasswordException("Contraseña incorrecta");
        } catch (DisabledException ex) {
            log.warn("La cuenta del usuario '{}' no está activada", request.userName(), ex);
            throw new AccountDisabledException("La cuenta no está activada");
        } catch (UsernameNotFoundException ex) {
            log.warn("No se encontró el usuario '{}'", request.userName(), ex);
            throw new UserNotFoundException(request.userName());
        } catch (Exception ex) {
            log.error("Error inesperado durante la autenticación del usuario '{}'", request.userName(), ex);
            throw new AuthenticationServiceException("Error interno durante la autenticación", ex);
        }
    }


    @Override
    public JwtAccessResponse refreshAccessToken(String refreshToken) {
        log.info("Validando refreshToken....");
        // 1. Validar refresh token
        jwtUtils.validateRefreshToken(refreshToken);

        //2. Extraer UserId
        String userId = jwtUtils.extractUserId(refreshToken);

        // 3. Buscar usuario
        User user = userRepository.findById(new ObjectId(userId))
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));

        // 4. Generar nuevo access token
        String newAccessToken = jwtUtils.generateToken(user);
        return new JwtAccessResponse(newAccessToken);
    }


    /**
     * Valida que los campos del request no sean nulos o vacíos.
     *
     * @param request Petición de login.
     * @throws InvalidPasswordException Si el usuario o contraseña están vacíos.
     */
    private void validateRequest(LoginRequest request) {
        if (request == null || request.userName() == null || request.userName().isBlank()
                || request.password() == null || request.password().isBlank()) {
            log.warn("Solicitud de autenticación inválida: username o password vacíos");
            throw new InvalidPasswordException("Usuario y contraseña son requeridos");
        }
    }

    /**
     * Carga los detalles del usuario desde el servicio de detalles de usuario.
     *
     * @param userName Nombre de usuario a buscar.
     * @return Detalles del usuario encontrados.
     * @throws UsernameNotFoundException si no se encuentra el usuario.
     */
    private UserDetails loadUserDetails(String userName) {
        log.debug("Cargando detalles del usuario '{}'", userName);
        return userDetailsService.loadUserByUsername(userName);
    }

    /**
     * Realiza la autenticación con el `AuthenticationManager`.
     *
     * @param request     Objeto con las credenciales.
     * @param userDetails Detalles previamente cargados del usuario.
     * @return Resultado de la autenticación.
     * @throws BadCredentialsException si las credenciales son incorrectas.
     */
    private Authentication performAuthentication(LoginRequest request, UserDetails userDetails) {
        log.info("Autenticando usuario '{}'", request.userName());
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.userName(),
                        request.password(),
                        userDetails.getAuthorities()
                )
        );
    }

    /**
     * Genera un token JWT para el usuario autenticado.
     *
     * @param user Usuario autenticado.
     * @return Token JWT válido para el usuario.
     */
    private JwtResponse generateJwtToken(User user) {
        log.debug("Generando JWT para el usuario '{}'", user.getUsername());
        return new JwtResponse(jwtUtils.generateToken(user), jwtUtils.generateRefreshToken(user));
    }


}
