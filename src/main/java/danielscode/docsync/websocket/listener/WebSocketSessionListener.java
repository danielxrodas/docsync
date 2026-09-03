package danielscode.docsync.websocket.listener;

import danielscode.docsync.collaboration.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionListener {

    private final CollaborationService collaborationService;

    // Fired when STOMP CONNECTED handshake completes (after our CONNECT frame)
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket session connected: {}", accessor.getSessionId());
    }

    // Fired when a WebSocket connection closes — covers both voluntary DISCONNECT
    // and abrupt drops (tab close, network failure).
    // We cannot know which room the session was in at the transport level,
    // so we ask CollaborationService to search all active rooms.
    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("WebSocket session disconnected: {}", sessionId);
        collaborationService.handleDisconnect(sessionId);
    }
}
