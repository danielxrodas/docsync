package danielscode.docsync.room;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/*
Created so Users cant join a room that they are currently in
 */

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomParticipantId implements Serializable {
    UUID roomId;
    UUID userId;
}
