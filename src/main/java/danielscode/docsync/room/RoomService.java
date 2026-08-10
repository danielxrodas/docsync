package danielscode.docsync.room;

import danielscode.docsync.document.DocumentService;
import danielscode.docsync.document.dto.DocumentResponse;
import danielscode.docsync.room.dto.CreateRoomRequest;
import danielscode.docsync.room.dto.JoinRoomResponse;
import danielscode.docsync.room.dto.RoomResponse;
import danielscode.docsync.shared.exception.ResourceNotFoundException;
import danielscode.docsync.user.User;
import danielscode.docsync.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final DocumentService documentService;
    private final UserService userService;


    public RoomResponse createRoom(CreateRoomRequest request, UUID creatorId) {
        User creator = userService.findById(creatorId);
        Room room = Room.builder()
                .name(request.name())
                .createdBy(creator)
                .active(true)
                .build();

        Room savedRoom =  roomRepository.save(room);
        documentService.createEmptyDocument(savedRoom);

        RoomParticipant participant = RoomParticipant.builder()
                .id(new RoomParticipantId(savedRoom.getId(), creator.getId()))
                .room(savedRoom)
                .user(creator)
                .joinedAt(java.time.Instant.now())
                .build();

        roomParticipantRepository.save(participant);

        return RoomResponse.from(savedRoom, 1);
    }

    public JoinRoomResponse joinRoom(UUID roomId, UUID userId) {
        Room room = roomRepository.findByIdAndActiveTrue(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found or inactive: " + roomId));
        User user = userService.findById(userId);

        if (!roomParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            RoomParticipant participant = RoomParticipant.builder()
                    .id(new RoomParticipantId(roomId, userId))
                    .room(room)
                    .user(user)
                    .joinedAt(java.time.Instant.now())
                    .build();

            roomParticipantRepository.save(participant);
        }

        DocumentResponse documentResponse = documentService.getDocumentByRoomId(roomId);
        long count = roomParticipantRepository.countByRoomId(roomId);

        return new JoinRoomResponse(
                RoomResponse.from(room, count),
                documentResponse.content(),
                documentResponse.version()
        );
    }

    public boolean isParticipant(UUID roomId, UUID userId) {
        return roomParticipantRepository.existsByRoomIdAndUserId(roomId, userId);
    }

    public RoomResponse getRoomById(UUID roomId) {
        Room room = roomRepository.findByIdAndActiveTrue(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        return RoomResponse.from(room, roomParticipantRepository.countByRoomId(roomId));
    }

    public List<RoomResponse> getAllActiveRooms() {
        List<Room> rooms = roomRepository.findByActiveTrue();
        return rooms.stream()
                .map(room -> RoomResponse.from(room, roomParticipantRepository.countByRoomId(room.getId())))
                .toList();
    }
}
