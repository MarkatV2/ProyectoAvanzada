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
 * Configuración centralizada para la carga de plantillas HTML utilizadas en correos electrónicos.
 * <p>
 * Esta clase registra cada plantilla como un bean de Spring, permitiendo su inyección donde se requiera.
 * Las plantillas se cargan desde el classpath (ubicadas en la carpeta <code>resources/templates</code>)
 * y se almacenan en memoria para mejorar el rendimiento durante el envío de correos frecuentes.
 * </p>
 */
@Configuration
@Slf4j
public class EmailTemplateConfig {

    /**
     * Carga la plantilla HTML para verificación de cuenta.
     *
     * @return Contenido de la plantilla como cadena de texto.
     */
    @Bean
    public String verificationEmailTemplate() {
        return cargarPlantilla("templates/verification-email.html");
    }

    /**
     * Carga la plantilla HTML para recuperación de contraseña.
     *
     * @return Contenido de la plantilla como cadena de texto.
     */
    @Bean
    public String resetPasswordEmailTemplate() {
        return cargarPlantilla("templates/reset-password-email.html");
    }

    /**
     * Carga la plantilla HTML para notificaciones de comentarios en reportes.
     *
     * @return Contenido de la plantilla como cadena de texto.
     */
    @Bean
    public String commentNotificationEmailTemplate() {
        return cargarPlantilla("templates/comment_notification.html");
    }

    @Bean
    public String reportNotificationEmailTemplate() {
        return cargarPlantilla("templates/report-notification.html");
    }

    /**
     * Método utilitario para cargar el contenido de un archivo HTML desde el classpath.
     * <p>
     * En caso de fallo durante la lectura, lanza una excepción para detener el arranque de la aplicación.
     * </p>
     *
     * @param ruta Ruta relativa dentro del classpath del archivo HTML.
     * @return Contenido del archivo como texto plano.
     * @throws RuntimeException Si ocurre un error al leer la plantilla.
     */
    private String cargarPlantilla(String ruta) {
        ClassPathResource recurso = new ClassPathResource(ruta);
        try {
            log.debug("Cargando plantilla de correo desde: {}", recurso.getPath());
            return FileCopyUtils.copyToString(
                    new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            log.error("Error al cargar plantilla de correo '{}'", ruta, e);
            throw new RuntimeException("Error al cargar plantilla: " + ruta, e);
        }
    }
}

