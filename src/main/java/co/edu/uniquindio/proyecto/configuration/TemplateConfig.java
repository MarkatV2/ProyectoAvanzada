package co.edu.uniquindio.proyecto.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Configuration
public class TemplateConfig {

    @Bean
    public String verificationEmailTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/verification-email.html");
        return FileCopyUtils.copyToString(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        );
    }
}