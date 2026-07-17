package danielscode.docsync.document.dto;

import jakarta.validation.constraints.NotNull;

public record SaveDocumentRequest(
        @NotNull String content,
        boolean createVersion
) {}