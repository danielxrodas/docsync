CREATE TABLE rooms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)  NOT NULL,
    created_by  UUID          NOT NULL REFERENCES users(id),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE room_participants (
    room_id     UUID         NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     UUID         NOT NULL REFERENCES users(id),
    joined_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (room_id, user_id)
);

CREATE INDEX idx_rooms_active      ON rooms(active) WHERE active = TRUE;
CREATE INDEX idx_participants_room ON room_participants(room_id);