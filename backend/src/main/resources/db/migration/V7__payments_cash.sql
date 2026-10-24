-- V7__payments_cash.sql
-- Cash payments (docs/payment-implementation-plan.md §6 P1). Card/refund columns come with their own migrations.

ALTER TABLE payments
    ADD COLUMN tip_amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (tip_amount >= 0),
    ADD COLUMN tendered_amount DECIMAL(10,2),   -- CASH only
    ADD COLUMN change_given    DECIMAL(10,2),   -- CASH only
    ADD COLUMN created_by      UUID REFERENCES users(id),
    ADD COLUMN updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT chk_payment_amount_positive CHECK (amount > 0);
