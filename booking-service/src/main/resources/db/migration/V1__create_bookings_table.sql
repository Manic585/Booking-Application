-- Shared trigger function for auto-updating updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE bookings (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID          NOT NULL,
    reference_id       UUID          NOT NULL,
    booking_type       VARCHAR(30)   NOT NULL,
    inventory_item_id  UUID          NOT NULL,
    booking_date       DATE          NOT NULL,
    status             VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    total_amount       NUMERIC(12,2) NOT NULL,
    idempotency_key    VARCHAR(64)   NOT NULL,
    failure_reason     TEXT,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_bookings_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_booking_status CHECK (
        status IN ('PENDING','INVENTORY_HELD','PAYMENT_PROCESSING','CONFIRMED','CANCELLED','FAILED')
    )
);

CREATE INDEX idx_bookings_user_id    ON bookings (user_id, created_at DESC);
CREATE INDEX idx_bookings_status     ON bookings (status);
CREATE INDEX idx_bookings_reference  ON bookings (reference_id, booking_date);

CREATE TRIGGER trg_bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
