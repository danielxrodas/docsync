package danielscode.docsync.room;

import danielscode.docsync.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "room_participants")
@Getter
@Setter
@NoArgsConstructor
public class RoomParticipant {

    @EmbeddedId
    private RoomParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId")
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Instant joinedAt;
}
