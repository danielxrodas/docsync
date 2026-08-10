package danielscode.docsync.document;

import danielscode.docsync.document.dto.DocumentResponse;
import danielscode.docsync.room.Room;
import danielscode.docsync.shared.exception.ResourceNotFoundException;
import danielscode.docsync.user.User;
import danielscode.docsync.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final UserService userService;

    public Document createEmptyDocument(Room room) {
        Document document = Document.builder()
                .room(room)
                .content("")
                .updatedAt(Instant.now())
                .build();
        return documentRepository.save(document);
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public Document saveDocument(UUID roomId, String content, UUID savedByUserId) {
        Document document = documentRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for room: " + roomId));
        document.setContent(content);
        document.setUpdatedAt(Instant.now());
        return documentRepository.save(document);
    }

    public void snapshotVersion(UUID roomId, UUID savedByUserId) {
        Document document = documentRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for room: " + roomId));
        User savedBy = userService.findById(savedByUserId);
        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .content(document.getContent())
                .savedAt(Instant.now())
                .savedBy(savedBy)
                .build();
        documentVersionRepository.save(version);
    }

    public DocumentResponse getDocumentByRoomId(UUID roomId) {
        Document document = documentRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for room: " + roomId));
        return new DocumentResponse(
                document.getId(),
                document.getRoom().getId(),
                document.getContent(),
                document.getUpdatedAt(),
                document.getVersion()
        );
    }

    public String getCurrentContent(UUID roomId) {
        return documentRepository.findByRoomId(roomId)
                .map(Document::getContent)
                .orElse("");
    }
}