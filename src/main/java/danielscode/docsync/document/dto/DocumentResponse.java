package danielscode.docsync.document.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID roomId,
        String content,
        Instant updatedAt,
        long version
) {}
