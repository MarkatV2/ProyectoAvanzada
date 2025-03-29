package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.user.JwtResponse;
import co.edu.uniquindio.proyecto.dto.user.LoginRequest;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {
    public JwtResponse authenticate(LoginRequest request);


}
