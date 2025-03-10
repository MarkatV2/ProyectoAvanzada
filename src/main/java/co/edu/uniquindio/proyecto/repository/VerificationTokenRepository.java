package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.User;
import co.edu.uniquindio.proyecto.entity.VerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {

    Optional<VerificationToken> findByToken(String token);

}
