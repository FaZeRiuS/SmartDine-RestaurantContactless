-- Seed a small set of allergens for demo purposes (idempotent).
-- Allergen codes are normalized (lowercase, ASCII): gluten, milk, egg, fish, nuts, soy.

WITH seed_allergens(dish_name, allergen) AS (
    VALUES
        -- Breakfast / pastries / dairy
        ('Сирники зі сметаною', 'milk'),
        ('Сирники зі сметаною', 'egg'),
        ('Сирники зі сметаною', 'gluten'),
        ('Гранола з йогуртом', 'milk'),
        ('Гранола з йогуртом', 'gluten'),
        ('Круасан з мигдалем', 'gluten'),
        ('Круасан з мигдалем', 'egg'),
        ('Круасан з мигдалем', 'milk'),
        ('Круасан з мигдалем', 'nuts'),
        ('Круасан з шинкою та сиром', 'gluten'),
        ('Круасан з шинкою та сиром', 'milk'),
        ('Круасан з шинкою та сиром', 'egg'),
        ('Французькі тости', 'gluten'),
        ('Французькі тости', 'egg'),
        ('Французькі тости', 'milk'),
        ('Йогурт з фруктами', 'milk'),

        -- Fish / seafood
        ('Тартар з лосося', 'fish'),
        ('Брускета з лососем та крем-сиром', 'fish'),
        ('Брускета з лососем та крем-сиром', 'milk'),
        ('Креветки темпура', 'gluten'),
        ('Лосось гриль', 'fish'),
        ('Юшка з риби', 'fish'),
        ('Устриці (6 шт.)', 'fish'),

        -- Pasta / wheat
        ('Паста Карбонара', 'gluten'),
        ('Паста Карбонара', 'egg'),
        ('Паста Карбонара', 'milk'),
        ('Паста Болоньєзе', 'gluten'),
        ('Лазанья болоньєзе', 'gluten'),
        ('Лазанья болоньєзе', 'milk'),

        -- Desserts (nuts, dairy, eggs, gluten)
        ('Київський торт', 'nuts'),
        ('Київський торт', 'egg'),
        ('Київський торт', 'milk'),
        ('Медовик', 'egg'),
        ('Медовик', 'milk'),
        ('Медовик', 'gluten'),
        ('Тірамісу', 'egg'),
        ('Тірамісу', 'milk'),
        ('Тірамісу', 'gluten'),
        ('Чізкейк Нью-Йорк', 'milk'),
        ('Чізкейк Нью-Йорк', 'egg'),
        ('Чізкейк Нью-Йорк', 'gluten'),
        ('Брауні з морозивом', 'gluten'),
        ('Брауні з морозивом', 'egg'),
        ('Брауні з морозивом', 'milk'),
        ('Брауні з морозивом', 'nuts')
)
INSERT INTO dish_allergens(dish_id, allergen)
SELECT d.id, sa.allergen
FROM seed_allergens sa
JOIN dish d ON d.name = sa.dish_name
WHERE NOT EXISTS (
    SELECT 1
    FROM dish_allergens da
    WHERE da.dish_id = d.id AND da.allergen = sa.allergen
);

