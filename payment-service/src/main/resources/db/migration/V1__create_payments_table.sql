-- Shared trigger function for auto-updating updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE payments (
    id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id             UUID          NOT NULL,
    user_id                UUID          NOT NULL,
    amount                 NUMERIC(12,2) NOT NULL,
    currency               VARCHAR(10)   NOT NULL DEFAULT 'INR',
    status                 VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_method         VARCHAR(20)   NOT NULL,
    payment_reference      VARCHAR(50),
    gateway_transaction_id VARCHAR(100),
    idempotency_key        VARCHAR(64)   NOT NULL,
    failure_reason         TEXT,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payment_status CHECK (
        status IN ('PENDING','PROCESSING','COMPLETED','FAILED','REFUNDED')
    ),
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_payments_booking_id ON payments (booking_id);
CREATE INDEX idx_payments_user_id    ON payments (user_id);
CREATE INDEX idx_payments_status     ON payments (status);

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
