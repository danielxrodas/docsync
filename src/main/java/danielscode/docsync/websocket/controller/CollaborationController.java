package danielscode.docsync.websocket.controller;

import danielscode.docsync.collaboration.service.CollaborationService;
import danielscode.docsync.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationService collaborationService;

    // Client sends: STOMP SEND to /app/room/{roomId}/join
    // Server broadcasts: JOIN_ROOM event to /topic/room/{roomId}
    @MessageMapping("/room/{roomId}/join")
    public void handleJoin(
            @DestinationVariable UUID roomId,
            @AuthenticationPrincipal UserPrincipal principal,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        collaborationService.addParticipant(
                roomId, sessionId, principal.getId(), principal.getUsername()
        );
    }

    // Client sends: STOMP SEND to /app/room/{roomId}/leave
    // Server broadcasts: LEAVE_ROOM event to /topic/room/{roomId}
    @MessageMapping("/room/{roomId}/leave")
    public void handleLeave(
            @DestinationVariable UUID roomId,
            @AuthenticationPrincipal UserPrincipal principal,
            SimpMessageHeaderAccessor headerAccessor) {

        collaborationService.removeParticipant(roomId, headerAccessor.getSessionId());
    }

    // Phase 4: handleEdit will go here
    // @MessageMapping("/room/{roomId}/edit")
    // public void handleEdit(...) { ... }

    // Phase 5: handleCursor will go here
    // @MessageMapping("/room/{roomId}/cursor")
    // public void handleCursor(...) { ... }
}
