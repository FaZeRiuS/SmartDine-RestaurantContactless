-- Convert money fields from REAL/float to NUMERIC(19,2)
-- This prevents rounding errors in price/total calculations.

ALTER TABLE dish
    ALTER COLUMN price TYPE NUMERIC(19, 2) USING ROUND(price::numeric, 2);

ALTER TABLE orders
    ALTER COLUMN total_price TYPE NUMERIC(19, 2) USING ROUND(total_price::numeric, 2);

