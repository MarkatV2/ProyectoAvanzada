package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.user.JwtResponse;
import co.edu.uniquindio.proyecto.dto.user.LoginRequest;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.auth.AccountDisabledException;
import co.edu.uniquindio.proyecto.exception.user.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.service.interfaces.AuthService;
import co.edu.uniquindio.proyecto.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de la autenticación de usuarios y generación de tokens JWT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImplements implements AuthService {

    private final AuthenticationManager authenticationManager;
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
    public JwtResponse authenticate(LoginRequest request) {
        log.info("Inicio de autenticación para el usuario: {}", request.userName());
        try {
            // Cargar detalles del usuario
            UserDetails userDetails = loadUserDetails(request.userName());

            // Realizar la autenticación usando el AuthenticationManager
            Authentication authentication = performAuthentication(request, userDetails);

            // Se espera que el principal sea una instancia de User
            User user = (User) authentication.getPrincipal();
            verifyUserStatus(user);

            // Generar token JWT para el usuario autenticado
            String token = generateJwtToken(user);
            log.info("Usuario {} autenticado exitosamente", request.userName());
            return new JwtResponse(token);
        } catch (BadCredentialsException ex) {
            log.error("Error de autenticación para {}: credenciales incorrectas", request.userName());
            throw new InvalidPasswordException("Contraseña Incorrecta");
        } catch (DisabledException ex) {
            log.error("Error de autenticación para {}: cuenta deshabilitada", request.userName());
            throw new AccountDisabledException("La cuenta no está activada");
        } catch (UsernameNotFoundException ex) {
            log.error("El usuario {} no fue encontrado", request.userName());
            throw new UserNotFoundException("Usuario no encontrado: " + request.userName());
        } catch (Exception ex) {
            log.error("Error inesperado durante la autenticación para {}: {}", request.userName(), ex.getMessage());
            throw new AuthenticationServiceException("Error durante la autenticación", ex);
        }
    }

    /**
     * Carga los detalles del usuario a partir de su nombre de usuario.
     *
     * @param userName Nombre de usuario.
     * @return Objeto {@code UserDetails} correspondiente al usuario.
     */
    private UserDetails loadUserDetails(String userName) {
        log.debug("Cargando detalles del usuario: {}", userName);
        return userDetailsService.loadUserByUsername(userName);
    }

    /**
     * Realiza la autenticación del usuario utilizando el AuthenticationManager.
     *
     * @param request     Objeto con las credenciales del usuario.
     * @param userDetails Detalles previamente cargados del usuario.
     * @return Objeto {@code Authentication} resultante de la autenticación.
     */
    private Authentication performAuthentication(LoginRequest request, UserDetails userDetails) {
        log.debug("Realizando autenticación para el usuario: {}", request.userName());
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.userName(),
                        request.password(),
                        userDetails.getAuthorities()
                )
        );
    }

    /**
     * Verifica que la cuenta del usuario esté activada.
     *
     * @param user Usuario a verificar.
     * @throws DisabledException si la cuenta del usuario no está activada.
     */
    private void verifyUserStatus(User user) {
        if (!user.isEnabled()) {
            log.warn("La cuenta del usuario {} no está activada", user.getUsername());
            throw new DisabledException("La cuenta no está activada");
        }
    }

    /**
     * Genera un token JWT para el usuario autenticado.
     *
     * @param user Usuario autenticado.
     * @return Token JWT generado.
     */
    private String generateJwtToken(User user) {
        log.debug("Generando token JWT para el usuario: {}", user.getUsername());
        return jwtUtils.generateToken(user);
    }
}
