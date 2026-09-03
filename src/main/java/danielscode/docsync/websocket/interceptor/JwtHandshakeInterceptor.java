package danielscode.docsync.websocket.interceptor;

import danielscode.docsync.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    //This is to help validation, how to handle it before the handshake between the server and user and after
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String token = extractToken(request.getURI().getQuery());

        if (token == null) {
            // No token in query string — defer auth to StompAuthChannelInterceptor
            // which will check the STOMP CONNECT headers instead
            log.debug("No token in WS handshake query, deferring to STOMP auth");
            return true;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Invalid JWT token during WS handshake from {}", request.getRemoteAddress());
            return false;
        }

        // Store validated identity in session attributes so StompAuthChannelInterceptor
        // can retrieve them on the STOMP CONNECT frame without re-parsing the JWT
        String username = jwtTokenProvider.extractUsername(token);
        UUID userId = jwtTokenProvider.extractUserId(token);
        attributes.put("username", username);
        attributes.put("userId", userId);
        log.debug("WS handshake authorized for user: {}", username);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // Nothing needed here
    }

    private String extractToken(String query) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                String value = param.substring("token=".length());
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }
}
