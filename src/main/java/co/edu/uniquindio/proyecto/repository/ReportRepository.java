package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.report.Report;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;


/**
 * Repositorio para la entidad {@link Report}, maneja las operaciones de acceso a datos
 * relacionados con los reportes en la colección "reports" de MongoDB.
 *
 * Este repositorio proporciona métodos para acceder a los reportes basados en varios
 * criterios, como el estado del reporte, la proximidad geográfica o la existencia de
 * reportes con título y descripción específicos.
 */
public interface ReportRepository extends MongoRepository<Report, ObjectId> {

    /**
     * Verifica si existe un reporte con el mismo título y descripción.
     * Este método se puede usar para evitar la creación de reportes duplicados.
     *
     * @param title Título del reporte.
     * @param description Descripción del reporte.
     * @return {@code true} si existe un reporte con el mismo título y descripción,
     *         {@code false} en caso contrario.
     */
    boolean existsByTitleAndDescription(String title, String description);


    @Override
    @Query("""
            {'_id': ?0,'reportStatus': { $ne: 'DELETED' }}""")
    Optional<Report> findById(ObjectId id);

     @Query("""
            {'reportStatus': { $ne: 'DELETED'}}""")
    Page<Report> findAllReports(Pageable pageable);


    @Query("""
            {'userId': ?1, 'reportStatus': { $ne: 'DELETED'}}""")
    Page<Report> findAllReportsByUserId(Pageable pageable, ObjectId userId);

    /**
     * Busca los reportes cercanos a una ubicación específica, dentro de una distancia máxima.
     * La búsqueda es realizada usando la indexación geoespacial 2DSPHERE en MongoDB.
     *
     * @param location Ubicación geográfica desde donde buscar los reportes cercanos.
     * @param maxDistanceInMeters Distancia máxima en metros dentro de la cual se buscan los reportes.
     * @param pageable Información de paginación (página y tamaño de la página).
     * @return Una página de reportes cercanos a la ubicación dada.
     */
    @Query("""
    {
        'location': {
            $near: {
                $geometry: ?0,
                $maxDistance: ?1
            }
        },
        'reportStatus': 'VERIFIED'
    }
""")
    Page<Report> findNearbyReports(
            GeoJsonPoint location,
            double maxDistanceInMeters,
            Pageable pageable
    );


    /**
     * Obtiene un reporte por su ID, solo si el estado del reporte no es "DELETED".
     * Esto asegura que no se recuperen reportes eliminados.
     *
     * @return Un {@link Optional} que contiene el reporte si existe, {@link Optional#empty()}
     *         si no se encuentra o si el reporte está marcado como "DELETED".
     */


    @Query("""
{
    'location': {
        $near: {
            $geometry: ?0,
            $maxDistance: ?1
        }
    },
    'reportStatus': 'VERIFIED',
    'categoryList.name': { $in: ?2 }
}
""")
    Page<Report> findNearbyReportsByCategoryNames(
            GeoJsonPoint location,
            double maxDistanceInMeters,
            List<String> categoryNames,
            Pageable pageable
    );

}

