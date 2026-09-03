package danielscode.docsync.collaboration.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomEventMessage(
        MessageType type,
        UUID roomId,
        String userId,
        String username,
        Instant timestamp,
        int participantCount
) {
}
