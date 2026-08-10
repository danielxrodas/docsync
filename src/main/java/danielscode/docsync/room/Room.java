package danielscode.docsync.room;

import danielscode.docsync.document.Document;
import danielscode.docsync.shared.entity.BaseEntity;
import danielscode.docsync.user.User;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseEntity {

    @Column(nullable = false, length=100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private boolean active = true;

    @OneToOne(mappedBy = "room", cascade  = CascadeType.ALL, orphanRemoval = true)
    private Document document;
}
