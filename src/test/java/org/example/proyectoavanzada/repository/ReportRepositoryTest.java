package org.example.proyectoavanzada.repository;


import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@DataMongoTest
@ContextConfiguration(classes = ProyectoApplication.class)
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        // Limpiar la base de datos para garantizar la repetibilidad de las pruebas
        reportRepository.deleteAll();

        // Creación manual del índice geoespacial en 'location'
        mongoTemplate.indexOps(Report.class)
                .ensureIndex(new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));


        // Lista para almacenar los reportes de prueba
        List<Report> reportes = new ArrayList<>();

        // Reporte 1: Ubicado en (10.0, 10.0), VERIFICADO, con categoría "CatA"
        Report r1 = new Report();
        r1.setId(new ObjectId());
        r1.setTitle("Reporte 1");
        r1.setDescription("Descripcion 1");
        r1.setLocation(new GeoJsonPoint(10.0, 10.0));
        r1.setReportStatus(ReportStatus.VERIFIED);
        r1.setCategoryList(List.of(new CategoryRef("CatA")));

        // Reporte 2: Ubicado en (10.001, 10.0), VERIFICADO, con categoría "CatB"
        Report r2 = new Report();
        r2.setId(new ObjectId());
        r2.setTitle("Reporte 2");
        r2.setDescription("Descripcion 2");
        r2.setLocation(new GeoJsonPoint(10.001, 10.0));
        r2.setReportStatus(ReportStatus.VERIFIED);
        r2.setCategoryList(List.of(new CategoryRef("CatB")));

        // Reporte 3: Ubicado en (20.0, 20.0), con estado DELETED, con categoría "CatC"
        Report r3 = new Report();
        r3.setId(new ObjectId());
        r3.setTitle("Reporte 3");
        r3.setDescription("Descripcion 3");
        r3.setLocation(new GeoJsonPoint(20.0, 20.0));
        r3.setReportStatus(ReportStatus.DELETED);
        r3.setCategoryList(List.of(new CategoryRef("CatC")));

        // Reporte 4: Ubicado en (10.002, 10.002), VERIFICADO, con categoría "CatA"
        Report r4 = new Report();
        r4.setId(new ObjectId());
        r4.setTitle("Reporte 4");
        r4.setDescription("Descripcion 4");
        r4.setLocation(new GeoJsonPoint(10.002, 10.002));
        r4.setReportStatus(ReportStatus.VERIFIED);
        r4.setCategoryList(List.of(new CategoryRef("CatA")));

        // Reporte 5: Ubicado en (10.003, 10.003), VERIFICADO, con categoría "CatC"
        Report r5 = new Report();
        r5.setId(new ObjectId());
        r5.setTitle("Reporte 5");
        r5.setDescription("Descripcion 5");
        r5.setLocation(new GeoJsonPoint(10.003, 10.003));
        r5.setReportStatus(ReportStatus.VERIFIED);
        r5.setCategoryList(List.of(new CategoryRef("CatC")));

        // Agregar los reportes a la lista
        reportes.add(r1);
        reportes.add(r2);
        reportes.add(r3);
        reportes.add(r4);
        reportes.add(r5);

        // Persistir los datos de prueba en la base de datos
        reportRepository.saveAll(reportes);
    }

    @Test
    @DisplayName("Existe reporte por título y descripción")
    void testExistsByTitleAndDescription() {
        // Arrange: Tomamos los valores de título y descripción de r1
        String title = "Reporte 1";
        String description = "Descripcion 1";

        // Act: Llamada al método del repositorio
        boolean exists = reportRepository.existsByTitleAndDescription(title, description);

        // Assert: Se verifica que exista el reporte
        assertTrue(exists, "El reporte con título y descripción indicados debe existir");

        // Verificar que para datos no existentes se retorne false
        boolean notExists = reportRepository.existsByTitleAndDescription("No existe", "No existe");
        assertFalse(notExists, "No debería existir un reporte con datos no registrados");
    }

    @Test
    @DisplayName("Buscar reportes cercanos (sin filtro de categorías)")
    void testFindNearbyReports() {
        // Arrange: Se define un punto de búsqueda cercano a r1, r2, r4 y r5
        GeoJsonPoint searchPoint = new GeoJsonPoint(10.0, 10.0);
        // Se define una distancia máxima amplia (en metros) para incluir los reportes cercanos
        double maxDistanceInMeters = 5000; // 5 km
        PageRequest pageable = PageRequest.of(0, 10);

        // Act: Se invoca el método del repositorio
        Page<Report> result = reportRepository.findNearbyReports(searchPoint, maxDistanceInMeters, pageable);

        // Assert: Solo se deben recuperar los reportes con estado VERIFIED y cercanos al punto
        // Se esperan 4 reportes: r1, r2, r4 y r5 (r3 se excluye por estar DELETED y/o estar lejos)
        assertEquals(4, result.getTotalElements(), "Se deben encontrar 4 reportes verificados cercanos");
    }

    @Test
    @DisplayName("Buscar reportes cercanos por nombres de categoría")
    void testFindNearbyReportsByCategoryNames() {
        // Arrange: Se define un punto de búsqueda y se filtra por la categoría "CatA"
        GeoJsonPoint searchPoint = new GeoJsonPoint(10.0, 10.0);
        double maxDistanceInMeters = 5000;
        PageRequest pageable = PageRequest.of(0, 10);
        List<String> categoryNames = List.of("CatA");

        // Act: Se invoca el método que filtra por categoría
        Page<Report> result = reportRepository.findNearbyReportsByCategoryNames(searchPoint, maxDistanceInMeters, categoryNames, pageable);

        // Assert: Solo se deben recuperar los reportes que tengan "CatA" en su lista de categorías.
        // Se esperan 2 reportes: r1 y r4
        assertEquals(2, result.getTotalElements(), "Se deben encontrar 2 reportes verificados cercanos con categoría 'CatA'");
    }
}
