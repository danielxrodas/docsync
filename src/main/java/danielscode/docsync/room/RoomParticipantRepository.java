package danielscode.docsync.room;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, RoomParticipantId> {
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);
    long countByRoomId(UUID roomId);
    List<RoomParticipant> findByRoomId(UUID roomId);
    @Modifying
    @Transactional
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);
}
