package co.edu.uniquindio.proyecto.entity.report;

import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "reports")
public class Report {

    @Id
    private ObjectId id;
    private String title;
    private String description;
    private List<CategoryRef> categoryList;
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;
    private ReportStatus reportStatus;
    private int importantVotes;
    private ObjectId userId;
    private LocalDateTime createdAt;

}
