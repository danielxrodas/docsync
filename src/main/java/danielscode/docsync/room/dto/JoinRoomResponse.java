package danielscode.docsync.room.dto;

public record JoinRoomResponse(
        RoomResponse room,
        String documentContent,
        long documentVersion
) {}
