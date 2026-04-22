-- Allow both authenticated UUID users and guest (session-based) identities in reviews.
-- Existing UUID values are preserved by casting to TEXT.

ALTER TABLE order_service_review
    ALTER COLUMN user_id TYPE TEXT USING user_id::text;

ALTER TABLE order_dish_review
    ALTER COLUMN user_id TYPE TEXT USING user_id::text;

