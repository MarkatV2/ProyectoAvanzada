package co.edu.uniquindio.proyecto.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Configuración para cargar la plantilla de correo de verificación.
 * <p>
 * Esta clase expone un bean que contiene el contenido de la plantilla HTML ubicada en el classpath.
 * </p>
 */
@Configuration
@Slf4j
public class TemplateConfig {

    /**
     * Carga la plantilla de correo de verificación desde el archivo "templates/verification-email.html".
     *
     * @return El contenido de la plantilla en formato String.
     * @throws RuntimeException si ocurre algún error al leer la plantilla.
     */
    @Bean
    public String verificationEmailTemplate() {
        ClassPathResource resource = new ClassPathResource("templates/verification-email.html");
        try {
            log.debug("Cargando plantilla de correo de verificación desde: {}", resource.getPath());
            return FileCopyUtils.copyToString(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            log.error("Error al cargar la plantilla de correo de verificación", e);
            throw new RuntimeException("Error al cargar la plantilla de correo de verificación", e);
        }
    }
}
