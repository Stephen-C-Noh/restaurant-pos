-- V9__order_cancel.sql
-- Why an order was cancelled and by whom.

ALTER TABLE orders ADD COLUMN cancel_reason TEXT;
ALTER TABLE orders ADD COLUMN cancelled_by UUID REFERENCES users(id);
