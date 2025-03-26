package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.report.Report;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;


public interface ReportRepository extends MongoRepository<Report, ObjectId> {

    boolean existsByTitleAndDescription (String title, String description); //Agregar Query de Reportes resuletos y estado del reporte
    @Query("""
        {
            'location': {
                $near: {
                    $geometry: ?0,
                    $maxDistance: ?1
                }
            },
        }
        """)
    Page<Report> findNearbyReports(
            GeoJsonPoint location,
            double maxDistanceInMeters,
            Pageable pageable
    );

}
