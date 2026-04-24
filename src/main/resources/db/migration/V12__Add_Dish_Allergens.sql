-- Add Dish Allergens (ElementCollection)
-- This enables public allergen display and filtering (exclude allergens).

CREATE TABLE IF NOT EXISTS dish_allergens (
    dish_id INT NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    allergen VARCHAR(64) NOT NULL,
    PRIMARY KEY (dish_id, allergen)
);

CREATE INDEX IF NOT EXISTS ix_dish_allergens_allergen
    ON dish_allergens(allergen);

CREATE INDEX IF NOT EXISTS ix_dish_allergens_dish_id
    ON dish_allergens(dish_id);

