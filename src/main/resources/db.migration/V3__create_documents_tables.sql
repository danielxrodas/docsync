CREATE TABLE documents (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id     UUID        NOT NULL UNIQUE REFERENCES rooms(id) ON DELETE CASCADE,
    content     TEXT        NOT NULL DEFAULT '',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE document_versions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID        NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    content     TEXT        NOT NULL,
    saved_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    saved_by    UUID        REFERENCES users(id)
);

CREATE INDEX idx_doc_versions_doc ON document_versions(document_id, saved_at DESC);