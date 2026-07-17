package danielscode.docsync.document;

import danielscode.docsync.room.Room;
import danielscode.docsync.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy="document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentVersion> versions;
}
