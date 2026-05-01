package danielscode.docsync.user;

import danielscode.docsync.shared.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true, length=50)
    private String username;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length=100)
    private String email;

    @JsonIgnore
    @Column(name="password_hash", nullable=false)
    private String passwordHash;

}