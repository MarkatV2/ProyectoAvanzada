package co.edu.uniquindio.proyecto.entity.report;

import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.util.Ownable;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Document(collection = "reports")
public class Report implements Ownable {

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
    private Set<ObjectId> likedUserIds = new HashSet<>();


    @Override
    public String getUserId() {
        return this.userId.toString();
    }
}
