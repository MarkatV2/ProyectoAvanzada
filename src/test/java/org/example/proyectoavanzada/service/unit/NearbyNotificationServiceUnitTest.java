package org.example.proyectoavanzada.service.unit;

import co.edu.uniquindio.proyecto.dto.notification.NotificationCreateDTO;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.implementations.NearbyNotificationService;
import co.edu.uniquindio.proyecto.service.interfaces.NotificationService;
import co.edu.uniquindio.proyecto.service.mapper.NotificationMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NearbyNotificationServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NearbyNotificationService nearbyNotificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --------------------- Pruebas del método público notifyUsersNearby ---------------------

    @Test
    void debeNotificarSoloUsuariosCercanosConUbicacionDiferenteAlAutor() {
        ObjectId authorId = new ObjectId();
        GeoJsonPoint reportLocation = new GeoJsonPoint(-75.0, 6.0);

        Report report = new Report();
        report.setUserId(authorId);
        report.setLocation(reportLocation);
        report.setId(new ObjectId());

        User userCercano = new User();
        userCercano.setId(new ObjectId());
        userCercano.setLocation(new GeoJsonPoint(-75.001, 6.001));
        userCercano.setNotificationRadiusKm(5.0);

        User userLejano = new User();
        userLejano.setId(new ObjectId());
        userLejano.setLocation(new GeoJsonPoint(-76.0, 7.0));
        userLejano.setNotificationRadiusKm(1.0);

        User userSinUbicacion = new User();
        userSinUbicacion.setId(new ObjectId());
        userSinUbicacion.setNotificationRadiusKm(5.0);

        User autor = new User();
        autor.setId(authorId);
        autor.setLocation(reportLocation);
        autor.setNotificationRadiusKm(5.0);

        when(userRepository.findAll()).thenReturn(List.of(userCercano, userLejano, userSinUbicacion, autor));

        NotificationCreateDTO dto = mock(NotificationCreateDTO.class);
        when(notificationMapper.buildFromReportForNearbyUser(report, userCercano.getId().toString())).thenReturn(dto);

        nearbyNotificationService.notifyUsersNearby(report);

        verify(notificationService, times(1)).notifyUser(dto);
    }

    @Test
    void noDebeLanzarExcepcionSiFallaUnaNotificacion() {
        ObjectId authorId = new ObjectId();
        GeoJsonPoint location = new GeoJsonPoint(-75.0, 6.0);

        Report report = new Report();
        report.setUserId(authorId);
        report.setLocation(location);
        report.setId(new ObjectId());

        User user = new User();
        user.setId(new ObjectId());
        user.setLocation(new GeoJsonPoint(-75.001, 6.001));
        user.setNotificationRadiusKm(5.0);

        when(userRepository.findAll()).thenReturn(List.of(user));
        NotificationCreateDTO dto = mock(NotificationCreateDTO.class);
        when(notificationMapper.buildFromReportForNearbyUser(any(), anyString())).thenReturn(dto);
        doThrow(new RuntimeException("WebSocket failed")).when(notificationService).notifyUser(dto);

        nearbyNotificationService.notifyUsersNearby(report);

        verify(notificationService).notifyUser(dto);
    }

    // --------------------- Pruebas del método privado isWithinRadius ---------------------

    @Test
    void isWithinRadiusDebeRetornarTrueParaUbicacionCercana() throws Exception {
        Method method = NearbyNotificationService.class.getDeclaredMethod("isWithinRadius", GeoJsonPoint.class, GeoJsonPoint.class, double.class);
        method.setAccessible(true);

        GeoJsonPoint puntoUsuario = new GeoJsonPoint(-75.001, 6.001);
        GeoJsonPoint puntoReporte = new GeoJsonPoint(-75.0, 6.0);

        boolean resultado = (boolean) method.invoke(nearbyNotificationService, puntoUsuario, puntoReporte, 1.0);
        assertTrue(resultado);
    }

    @Test
    void isWithinRadiusDebeRetornarFalseParaUbicacionLejana() throws Exception {
        Method method = NearbyNotificationService.class.getDeclaredMethod("isWithinRadius", GeoJsonPoint.class, GeoJsonPoint.class, double.class);
        method.setAccessible(true);

        GeoJsonPoint puntoUsuario = new GeoJsonPoint(-76.0, 7.0);
        GeoJsonPoint puntoReporte = new GeoJsonPoint(-75.0, 6.0);

        boolean resultado = (boolean) method.invoke(nearbyNotificationService, puntoUsuario, puntoReporte, 0.5);
        assertFalse(resultado);
    }

    // --------------------- Pruebas del método privado haversine ---------------------

    @Test
    void haversineDebeCalcularDistanciaCorrecta() throws Exception {
        Method method = NearbyNotificationService.class.getDeclaredMethod("haversine", double.class, double.class, double.class, double.class);
        method.setAccessible(true);

        // Medellín (-75.0, 6.0) y punto cercano (-75.001, 6.001)
        double distancia = (double) method.invoke(nearbyNotificationService, 6.0, -75.0, 6.001, -75.001);

        // Se espera una distancia muy pequeña
        assertTrue(distancia < 0.2 && distancia > 0.0);
    }

    @Test
    void haversineDebeRetornarCeroSiSonLasMismasCoordenadas() throws Exception {
        Method method = NearbyNotificationService.class.getDeclaredMethod("haversine", double.class, double.class, double.class, double.class);
        method.setAccessible(true);

        double distancia = (double) method.invoke(nearbyNotificationService, 6.0, -75.0, 6.0, -75.0);
        assertEquals(0.0, distancia, 0.0001);
    }
}
