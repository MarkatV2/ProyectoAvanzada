package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.user.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    @Query("{ 'accountStatus' : { $ne: 'DELETED' } }")
    Page<User> findAll(Pageable pageable);

    @Query("{ '_id' : ?0, 'accountStatus' : { $ne: 'DELETED' } }")
    Optional<User> findById(ObjectId id);

    @Query("{ 'email' : ?0, 'accountStatus' : { $ne: 'DELETED' } }")
    Optional<User> findByEmail(String email);

}