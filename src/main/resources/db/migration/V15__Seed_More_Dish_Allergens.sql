-- Extend allergen seeding so that popular/recommended dishes are more likely
-- to have visible allergen chips in the UI (idempotent).

WITH seed_allergens(dish_name, allergen) AS (
    VALUES
        -- Ukrainian classics / common allergens
        ('Борщ український', 'milk'),
        ('Вареники з картоплею', 'gluten'),
        ('Вареники з вишнею', 'gluten'),
        ('Деруни зі сметаною', 'milk'),
        ('Деруни зі сметаною', 'egg'),

        -- Burgers / bread
        ('Бургер з яловичиною', 'gluten'),
        ('Курячий бургер', 'gluten'),
        ('Фіш-енд-чіпс', 'gluten'),

        -- Coffee with milk (common in orders)
        ('Капучино', 'milk'),
        ('Лате', 'milk'),
        ('Матча-лате', 'milk'),
        ('Какао', 'milk'),

        -- Cheese-based dishes
        ('Піца Маргарита (міні)', 'milk'),
        ('Піца Маргарита (міні)', 'gluten'),
        ('Піца Пепероні (міні)', 'milk'),
        ('Піца Пепероні (міні)', 'gluten')
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

