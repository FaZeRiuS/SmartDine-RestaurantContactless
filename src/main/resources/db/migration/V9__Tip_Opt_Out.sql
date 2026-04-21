-- Add a flag to remember customer's tip opt-out per order
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS tip_opt_out BOOLEAN NOT NULL DEFAULT FALSE;

