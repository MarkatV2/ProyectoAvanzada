package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import io.micrometer.observation.ObservationFilter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para gestionar los códigos de verificación utilizados en procesos como
 * activación de cuenta, recuperación de contraseña, o verificación de identidad.
 *
 * <p>
 * Este repositorio se encarga de las operaciones de acceso a datos relacionadas con
 * la entidad {@link VerificationCode}.
 * Aplica un enfoque de "responsabilidad única", limitando su alcance a la persistencia y consulta
 * de códigos de verificación.
 * </p>
 *
 * <p>
 * Este tipo de repositorio es útil en contextos donde se requiere validar una acción sensible
 * a través de un código único, asegurando la trazabilidad por usuario.
 * </p>
 */
@Repository
public interface VerificationCodeRepository extends MongoRepository<VerificationCode, String> {

    /**
     * Busca un código de verificación específico.
     *
     * @param code El código de verificación.
     * @return Un {@link Optional} con el objeto {@link VerificationCode} si existe.
     */
    Optional<VerificationCode> findByCode(String code);

    /**
     * Elimina todos los códigos de verificación asociados a un usuario.
     *
     * @param userId ID del usuario.
     */
    void deleteAllByUserId(ObjectId userId);
}
