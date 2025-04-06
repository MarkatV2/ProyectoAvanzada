package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReportStatusHistoryRepository extends MongoRepository<ReportStatusHistory, ObjectId> {
}
