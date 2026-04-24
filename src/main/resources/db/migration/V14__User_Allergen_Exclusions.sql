-- Per-user allergen exclusions (customer preferences).
-- Used to filter out allergenic dishes across all menus/recommendations for authenticated users.

CREATE TABLE IF NOT EXISTS user_allergen_exclusions (
    user_id TEXT NOT NULL,
    allergen VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, allergen)
);

CREATE INDEX IF NOT EXISTS ix_user_allergen_exclusions_user_id
    ON user_allergen_exclusions(user_id);

