package danielscode.docsync.room.dto;

import danielscode.docsync.room.Room;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        String createdByUsername,
        Instant createdAt,
        boolean active,
        long participantCount
) {
    public static RoomResponse from(Room room, long count) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getCreatedBy().getUsername(),
                room.getCreatedAt(),
                room.isActive(),
                count
        );
    }
}