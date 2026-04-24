-- 1. Add preparation_time to dish table
ALTER TABLE dish ADD COLUMN preparation_time INT DEFAULT 15 NOT NULL;

-- 2. Add estimated_ready_time to orders table
ALTER TABLE orders ADD COLUMN estimated_ready_time TIMESTAMPTZ;

-- 3. Update specific dishes with accurate preparation times based on complexity

-- Steaks & complex dishes (30-35 mins)
UPDATE dish SET preparation_time = 30 WHERE name LIKE '%Стейк%' OR name = 'Свинячі реберця BBQ';
UPDATE dish SET preparation_time = 35 WHERE name = 'Устриці (6 шт.)'; -- Just as an example of long prep or raw bar wait

-- Hot main courses (25 mins)
UPDATE dish SET preparation_time = 25 
WHERE name IN (
    'Котлета по-київськи', 
    'Різото з морепродуктами', 
    'Різото з грибами', 
    'Лазанья болоньєзе', 
    'Філе тріски з пюре', 
    'Шніцель по-віденськи', 
    'Лосось гриль',
    'Мідії у вершковому соусі',
    'Фіш-енд-чіпс'
);

-- Burgers, pasta, soups, breakfast items (20 mins)
UPDATE dish SET preparation_time = 20 
WHERE name LIKE '%бургер%' OR name LIKE '%Бургер%' 
   OR name LIKE '%Паста%' 
   OR name LIKE '%піца%' OR name LIKE '%Піца%' 
   OR name LIKE '%суп%' OR name LIKE '%Суп%' 
   OR name IN (
       'Борщ український', 'Солянка м''ясна', 'Юшка з риби',
       'Куряче філе гриль', 'Курка теріякі', 'Бефстроганов', 'Плов з куркою',
       'Вареники з картоплею', 'Вареники з вишнею', 'Пельмені домашні',
       'Голубці', 'Гречка з тушкованою яловичиною', 'Овочеве рагу',
       'Яйця Бенедикт', 'Омлет з сиром та зеленню', 'Скрембл з лососем', 'Шакшука',
       'Млинці з лососем'
   );

-- Quick appetizers, simple breakfasts & cold desserts (10 mins)
UPDATE dish SET preparation_time = 10 
WHERE name IN (
    'Брускета з томатами та базиліком', 'Брускета з лососем та крем-сиром',
    'Хумус з пітою', 'Сирна тарілка', 'М''ясна тарілка',
    'Паштет з курячої печінки', 'Оселедець з цибулею та картоплею',
    'Сало з часником та гірчицею', 'Мариновані оливки та маслини',
    'Круасан з мигдалем', 'Круасан з шинкою та сиром',
    'Вівсянка з ягодами та медом', 'Гранола з йогуртом', 'Йогурт з фруктами',
    'Морозиво асорті', 'Морозиво дитяче', 'Профітролі', 'Макарони'
);

-- Drinks and beverages (5 mins)
UPDATE dish SET preparation_time = 5 
WHERE name IN (
    'Еспресо', 'Американо', 'Капучино', 'Лате', 'Матча-лате',
    'Чай чорний', 'Чай зелений', 'Лимонад домашній',
    'Апельсиновий фреш', 'Яблучний фреш', 'Морс ягідний',
    'Вода мінеральна газована', 'Вода мінеральна негазована', 'Какао',
    'Мохіто', 'Маргарита', 'Апероль Шприц', 'Негроні', 'Олд Фешн',
    'Пиво крафтове', 'Вино червоне келих', 'Вино біле келих'
);

-- (Anything else remains at the default 15 minutes)
