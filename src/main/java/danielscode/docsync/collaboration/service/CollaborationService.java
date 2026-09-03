package danielscode.docsync.collaboration.service;

import danielscode.docsync.collaboration.dto.MessageType;
import danielscode.docsync.collaboration.dto.RoomEventMessage;
import danielscode.docsync.collaboration.model.RoomState;
import danielscode.docsync.document.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentService documentService;

    // All active rooms that currently have at least one connected user.
    // Key: roomId, Value: live in-memory state for that room.
    // ConcurrentHashMap so multiple WebSocket threads can safely read/write the map.
    private final ConcurrentHashMap<UUID, RoomState> activeRooms = new ConcurrentHashMap<>();

    @Value("${app.collaboration.max-version-lag:10}")
    private int maxVersionLag;

    // ── Room state lifecycle ────────────────────────────────────────────────

    public RoomState getOrCreateRoomState(UUID roomId) {
        return activeRooms.computeIfAbsent(roomId, id -> {
            // Load the last-persisted content from DB as the starting point
            String content = documentService.getCurrentContent(id);
            log.info("Initializing in-memory state for room {}", id);
            return new RoomState(id, content);
        });
    }

    // ── Participant management ──────────────────────────────────────────────

    public void addParticipant(UUID roomId, String sessionId, UUID userId, String username) {
        RoomState state = getOrCreateRoomState(roomId);
        state.getSessionToUser().put(sessionId, userId);
        state.getUserIdToUsername().put(userId, username);
        broadcastRoomEvent(roomId, MessageType.JOIN_ROOM, userId, username, state);
        log.info("User {} ({}) joined room {} via session {}", username, userId, roomId, sessionId);
    }

    public void removeParticipant(UUID roomId, String sessionId) {
        RoomState state = activeRooms.get(roomId);
        if (state == null) return;

        UUID userId = state.getSessionToUser().remove(sessionId);
        if (userId == null) return;

        String username = state.getUserIdToUsername().get(userId);

        // Only broadcast LEAVE and remove the username if this user has NO remaining sessions
        boolean stillConnected = state.getSessionToUser().containsValue(userId);
        if (!stillConnected) {
            state.getUserIdToUsername().remove(userId);
            broadcastRoomEvent(roomId, MessageType.LEAVE_ROOM, userId, username, state);
            log.info("User {} ({}) left room {}", username, userId, roomId);
        }

        // If no sessions remain in the room, flush content to DB and evict from memory
        if (state.getSessionToUser().isEmpty()) {
            log.info("Room {} is now empty, flushing to DB and evicting from memory", roomId);
            flushRoomToDb(roomId, state);
            activeRooms.remove(roomId);
        }
    }

    public void handleDisconnect(String sessionId) {
        // Abrupt disconnect (tab close, network drop) — search all active rooms
        // for this session ID and remove it
        activeRooms.forEach((roomId, state) -> {
            if (state.getSessionToUser().containsKey(sessionId)) {
                removeParticipant(roomId, sessionId);
            }
        });
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    // Runs every 30 seconds. Flushes any rooms whose in-memory content has changed
    // since the last save. Uses tryLock so the scheduler never blocks waiting for
    // a room that is under heavy edit load — it will retry next cycle.
    @Scheduled(fixedDelayString = "${app.collaboration.flush-interval-ms:30000}")
    public void flushDirtyRooms() {
        if (activeRooms.isEmpty()) return;
        log.debug("Running scheduled flush for {} active rooms", activeRooms.size());

        activeRooms.forEach((roomId, state) -> {
            if (!state.isDirty()) return;

            boolean locked = false;
            try {
                locked = state.getEditLock().tryLock(100, TimeUnit.MILLISECONDS);
                if (!locked) {
                    log.debug("Skipping flush for room {} — lock contention, will retry next cycle", roomId);
                    return;
                }
                String content = String.join("\n", state.getContentLines());
                state.setDirty(false);
                state.setLastFlushTime(Instant.now());
                // Unlock before the DB write — never hold the edit lock during I/O
                state.getEditLock().unlock();
                locked = false;
                documentService.saveDocument(roomId, content, null);
                log.debug("Flushed room {} to DB", roomId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Failed to flush room {} to DB: {}", roomId, e.getMessage());
            } finally {
                if (locked) state.getEditLock().unlock();
            }
        });
    }

    private void flushRoomToDb(UUID roomId, RoomState state) {
        if (!state.isDirty()) return;
        try {
            String content = String.join("\n", state.getContentLines());
            documentService.saveDocument(roomId, content, null);
            state.setDirty(false);
            state.setLastFlushTime(Instant.now());
        } catch (Exception e) {
            log.error("Failed to flush room {} on empty: {}", roomId, e.getMessage());
        }
    }

    // ── Broadcasting ─────────────────────────────────────────────────────────

    private void broadcastRoomEvent(UUID roomId, MessageType type,
                                    UUID userId, String username, RoomState state) {
        RoomEventMessage msg = new RoomEventMessage(
                type,
                roomId,
                userId.toString(),
                username,
                Instant.now(),
                state.getSessionToUser().size()
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, msg);
    }
}
