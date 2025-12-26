-- V2__add_idempotency.sql
-- Add idempotency and offline support

-- Add idempotency fields to orders
ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(36) UNIQUE;
ALTER TABLE orders ADD COLUMN client_timestamp TIMESTAMP;
ALTER TABLE orders ADD COLUMN sync_status VARCHAR(20) DEFAULT 'SYNCED'
    CHECK (sync_status IN ('SYNCED', 'PENDING', 'CONFLICT'));

CREATE INDEX idx_orders_idempotency ON orders(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Add idempotency to payments
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(36) UNIQUE;
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Request deduplication cache (for short-term idempotency)
CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(36) PRIMARY KEY,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID NOT NULL,
    response_body JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_records(expires_at);

-- Event Outbox Pattern
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
