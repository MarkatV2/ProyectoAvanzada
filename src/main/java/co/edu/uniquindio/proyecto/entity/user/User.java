package co.edu.uniquindio.proyecto.entity.user;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Document(collection = "users")
public class User implements UserDetails {
    @Id
    private ObjectId id;
    private String email;
    private String password;
    private String fullName;
    private LocalDateTime dateBirth;
    private LocalDateTime createdAt;
    private Rol rol;
    private AccountStatus accountStatus;
    private String cityOfResidence;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.DELETED;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == AccountStatus.ACTIVATED;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

}

