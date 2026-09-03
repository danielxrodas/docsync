package danielscode.docsync.websocket.interceptor;

import danielscode.docsync.security.JwtTokenProvider;
import danielscode.docsync.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor

//It’s basically a security checkpoint for Connection Frame.
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String username = null;

            // Path 1: JwtHandshakeInterceptor already validated the token
            // and stored identity in session attributes
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                username = (String) sessionAttributes.get("username");
            }

            // Path 2: Token passed as STOMP CONNECT header (Authorization: Bearer TOKEN)
            // Used when the token is not in the URL (e.g. some clients)
            if (username == null) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (jwtTokenProvider.validateToken(token)) {
                        username = jwtTokenProvider.extractUsername(token);
                        UUID userId = jwtTokenProvider.extractUserId(token);
                        if (sessionAttributes != null) {
                            sessionAttributes.put("username", username);
                            sessionAttributes.put("userId", userId);
                        }
                    }
                }
            }

            if (username == null) {
                throw new AccessDeniedException("No valid authentication for WebSocket connection");
            }

            // Load full UserDetails and set as the STOMP session principal
            // This makes @AuthenticationPrincipal work in every @MessageMapping method
            UserDetails userDetails = userService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            accessor.setUser(auth);
            log.debug("STOMP CONNECT authenticated for user: {}", username);
        }

        return message;
    }
}
