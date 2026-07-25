-- Shared trigger function for auto-updating updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE inventory_items (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id     UUID         NOT NULL,
    seat_class       VARCHAR(30)  NOT NULL,
    label            VARCHAR(50)  NOT NULL,
    available_date   DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    booking_id       UUID,
    hold_expires_at  TIMESTAMPTZ,
    price            NUMERIC(12,2) NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_status CHECK (status IN ('AVAILABLE','HELD','BOOKED','CANCELLED'))
);

-- Composite index for availability queries (most common read path)
CREATE INDEX idx_inv_class_date_status ON inventory_items (seat_class, available_date, status);
CREATE INDEX idx_inv_ref_date        ON inventory_items (reference_id, available_date);
CREATE INDEX idx_inv_booking         ON inventory_items (booking_id) WHERE booking_id IS NOT NULL;
CREATE INDEX idx_inv_held_expires    ON inventory_items (hold_expires_at) WHERE status = 'HELD';

CREATE TRIGGER trg_inventory_updated_at
    BEFORE UPDATE ON inventory_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
