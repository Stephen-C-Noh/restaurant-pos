-- V8__outbox_relay_columns.sql
-- Columns the outbox relay needs: where to publish, and how often it failed.

ALTER TABLE outbox_events ADD COLUMN attempts INT NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN last_error TEXT;
ALTER TABLE outbox_events ADD COLUMN topic VARCHAR(100);
