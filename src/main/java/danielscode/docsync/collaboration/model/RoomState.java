package danielscode.docsync.collaboration.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
// An in-memory Java object that holds everything about a live room: the current document text (split into lines), who is connected, the version counter, and the edit lock.
public class RoomState {

    private final UUID roomId;
    private final List<String> contentLines;
    private final AtomicLong currentVersion;
    private final ConcurrentHashMap<String, UUID> sessionToUser;
    private final ConcurrentHashMap<UUID, String> userIdToUsername;
    private volatile boolean dirty;
    private volatile Instant lastFlushTime;
    private final ReentrantLock editLock;

    public RoomState(UUID roomId, String initialContent) {
        this.roomId = roomId;
        this.contentLines = new ArrayList<>(Arrays.asList(initialContent.split("\n", -1)));
        this.currentVersion = new AtomicLong(0);
        this.sessionToUser = new ConcurrentHashMap<>();
        this.userIdToUsername = new ConcurrentHashMap<>();
        this.dirty = false;
        this.lastFlushTime = Instant.now();
        this.editLock = new ReentrantLock();
    }

    public UUID getRoomId() { return roomId; }
    public List<String> getContentLines() { return contentLines; }
    public AtomicLong getCurrentVersion() { return currentVersion; }
    public ConcurrentHashMap<String, UUID> getSessionToUser() { return sessionToUser; }
    public ConcurrentHashMap<UUID, String> getUserIdToUsername() { return userIdToUsername; }
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
    public Instant getLastFlushTime() { return lastFlushTime; }
    public void setLastFlushTime(Instant lastFlushTime) { this.lastFlushTime = lastFlushTime; }
    public ReentrantLock getEditLock() { return editLock; }
}