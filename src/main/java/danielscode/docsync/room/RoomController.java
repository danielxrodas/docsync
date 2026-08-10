package danielscode.docsync.room;

import danielscode.docsync.room.dto.CreateRoomRequest;
import danielscode.docsync.room.dto.JoinRoomResponse;
import danielscode.docsync.room.dto.RoomResponse;
import danielscode.docsync.security.UserPrincipal;
import danielscode.docsync.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RoomResponse response = roomService.createRoom(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> listRooms() {
        List<RoomResponse> rooms = roomService.getAllActiveRooms();
        return ResponseEntity.ok(ApiResponse.success("Rooms retrieved", rooms));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(
            @PathVariable UUID roomId) {
        RoomResponse room = roomService.getRoomById(roomId);
        return ResponseEntity.ok(ApiResponse.success("Room retrieved", room));
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<ApiResponse<JoinRoomResponse>> joinRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal UserPrincipal principal) {
        JoinRoomResponse response = roomService.joinRoom(roomId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Joined room", response));
    }
}