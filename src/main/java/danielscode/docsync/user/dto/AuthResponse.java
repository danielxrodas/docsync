package danielscode.docsync.user.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UUID userId,
        String username
) {
}
