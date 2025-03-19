package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends MongoRepository<VerificationCode, String> {

    Optional<VerificationCode> findByCode(String code);

    ObservationFilter findByCodeAndUserId(String code, String userId);
}
