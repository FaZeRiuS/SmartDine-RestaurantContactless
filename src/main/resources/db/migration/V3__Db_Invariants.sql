-- Add DB invariants and performance indexes

-- Ensure 1 cart per user (prevents duplicates under race conditions)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ux_carts_user_id'
    ) THEN
        ALTER TABLE carts
            ADD CONSTRAINT ux_carts_user_id UNIQUE (user_id);
    END IF;
END$$;

-- Speed up user order history / active order lookups
CREATE INDEX IF NOT EXISTS ix_orders_user_id ON orders(user_id);

-- Speed up staff dashboards and lists by status + time
CREATE INDEX IF NOT EXISTS ix_orders_status_created_at ON orders(status, created_at DESC);

