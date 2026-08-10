package danielscode.docsync.document;

import danielscode.docsync.document.dto.DocumentResponse;
import danielscode.docsync.document.dto.SaveDocumentRequest;
import danielscode.docsync.room.RoomService;
import danielscode.docsync.security.UserPrincipal;
import danielscode.docsync.shared.exception.UnauthorizedException;
import danielscode.docsync.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final RoomService roomService;

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!roomService.isParticipant(roomId, principal.getId())) {
            throw new UnauthorizedException("You are not a participant of this room");
        }
        DocumentResponse response = documentService.getDocumentByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved", response));
    }

    @PutMapping("/{roomId}/save")
    public ResponseEntity<ApiResponse<DocumentResponse>> saveDocument(
            @PathVariable UUID roomId,
            @Valid @RequestBody SaveDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!roomService.isParticipant(roomId, principal.getId())) {
            throw new UnauthorizedException("You are not a participant of this room");
        }
        documentService.saveDocument(roomId, request.content(), principal.getId());
        if (request.createVersion()) {
            documentService.snapshotVersion(roomId, principal.getId());
        }
        DocumentResponse response = documentService.getDocumentByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success("Document saved", response));
    }
}