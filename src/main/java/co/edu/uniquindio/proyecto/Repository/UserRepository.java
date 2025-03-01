package co.edu.uniquindio.proyecto.Repository;

import co.edu.uniquindio.proyecto.Entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Page<User> findAll(Pageable pageable);
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);

}