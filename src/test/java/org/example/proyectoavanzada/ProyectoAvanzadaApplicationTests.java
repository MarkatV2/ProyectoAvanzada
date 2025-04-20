package org.example.proyectoavanzada;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = ProyectoApplication.class)
class ProyectoAvanzadaApplicationTests {

    @MockitoBean
    private EmailService emailService;

    @Test
    void contextLoads() {
    }
}