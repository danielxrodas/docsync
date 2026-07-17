package danielscode.docsync.document;

import danielscode.docsync.room.Room;
import danielscode.docsync.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DocumentService {
    public DocumentRepository documentRepository;
    private DocumentVersionRepository documentVersionRepository;
    private UserService userService;

    public Document createEmptyDocument(Room room) {
         Document document = Document.builder()
                .room(room)
                .content("")
                .updatedAt(java.time.Instant.now())
                .build();
         documentRepository.save(document);
         return document;
    }

    public Document saveDocument(UUID roomId, String content, UUID savedByUserId) {
        Document document = documentRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Document not found for room: " + roomId));
        document.setContent(content);
        document.setUpdatedAt(java.time.Instant.now());
        documentRepository.save(document);
        return document;
    }
    }
