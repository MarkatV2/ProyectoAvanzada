package org.example.proyectoavanzada.util;

import co.edu.uniquindio.proyecto.dto.user.JwtResponse;
import co.edu.uniquindio.proyecto.dto.user.LoginRequest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
public class LoginUtils {

    private final TestRestTemplate restTemplate;

    public LoginUtils(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String obtenerTokenAdmin() {
        return obtenerToken("admin@example.com", "admin123");
    }

    public String obtenerTokenUsuario() {
        return obtenerToken("user@example.com", "user123");
    }

    private String obtenerToken(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JwtResponse> response = restTemplate.exchange(
                "/api/v1/auth/sessions",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                JwtResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody().token();
        }
        throw new RuntimeException("Error al obtener token para pruebas");
    }

    public HttpHeaders crearHeadersConToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public String obtenerTokenUsuario(String mail, String password) {
        return obtenerToken(mail, password);
    }
}