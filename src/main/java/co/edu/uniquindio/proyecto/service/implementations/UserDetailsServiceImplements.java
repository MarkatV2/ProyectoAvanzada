package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link UserDetailsService} para cargar los detalles de usuario a partir del correo electrónico.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImplements implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga los detalles del usuario a partir del correo electrónico.
     *
     * @param email Correo electrónico del usuario.
     * @return Los detalles del usuario.
     * @throws UserNotFoundException Si no se encuentra un usuario con el correo proporcionado.
     */
    @Override
    public UserDetails loadUserByUsername(String email) {
        log.debug("Cargando detalles del usuario para el email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}

