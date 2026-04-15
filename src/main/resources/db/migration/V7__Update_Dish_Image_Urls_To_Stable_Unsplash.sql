-- Replace unstable Unsplash Source URLs with stable images.unsplash.com URLs.
-- Prod-safe: updates only dishes that still use source.unsplash.com.

UPDATE dish
SET image_url = CASE
    -- Ukrainian classics / soups
    WHEN name ILIKE '%борщ%' THEN 'https://images.unsplash.com/photo-1543353071-10c8ba85a904?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%солянк%' THEN 'https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%бульйон%' OR name ILIKE '%локшиною%' THEN 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%крем-суп%' OR name ILIKE '%суп%' THEN 'https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%вареники%' OR name ILIKE '%пельмені%' THEN 'https://images.unsplash.com/photo-1604908554028-4bba6a8f0e2e?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%деруни%' THEN 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%голубц%' THEN 'https://images.unsplash.com/photo-1604908554028-4bba6a8f0e2e?auto=format&fit=crop&w=900&q=75'

    -- Appetizers / salads
    WHEN name ILIKE '%брускет%' THEN 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%тартар%' THEN 'https://images.unsplash.com/photo-1551183053-bf91a1d81141?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%хумус%' THEN 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%цезар%' OR name ILIKE '%капрезе%' OR name ILIKE '%салат%' OR name ILIKE '%вінегрет%' OR name ILIKE '%олів''є%' THEN 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%сало%' OR name ILIKE '%оселедець%' OR name ILIKE '%форшмак%' THEN 'https://images.unsplash.com/photo-1604908177522-43259c59e86e?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%сирна тарілка%' OR name ILIKE '%м''ясна тарілка%' THEN 'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?auto=format&fit=crop&w=900&q=75'

    -- Pasta / risotto / lasagna
    WHEN name ILIKE '%паста%' OR name ILIKE '%карбонара%' OR name ILIKE '%болоньєзе%' OR name ILIKE '%альфредо%' THEN 'https://images.unsplash.com/photo-1473093226795-af9932fe5856?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%різото%' THEN 'https://images.unsplash.com/photo-1604908177522-43259c59e86e?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%лазанья%' THEN 'https://images.unsplash.com/photo-1608756687911-aa1599ab3bd9?auto=format&fit=crop&w=900&q=75'

    -- Meat / grill
    WHEN name ILIKE '%стейк%' OR name ILIKE '%шніцель%' OR name ILIKE '%бефстроганов%' OR name ILIKE '%реберця%' THEN 'https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%кур%' OR name ILIKE '%котлета%' OR name ILIKE '%плов%' THEN 'https://images.unsplash.com/photo-1555992336-03a23c6fce75?auto=format&fit=crop&w=900&q=75'

    -- Fish / seafood
    WHEN name ILIKE '%лосос%' THEN 'https://images.unsplash.com/photo-1485921325833-c519f76c4927?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%тріск%' OR name ILIKE '%фіш%' THEN 'https://images.unsplash.com/photo-1706711053549-f52f73a8960c?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%мідії%' OR name ILIKE '%устриці%' THEN 'https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%креветк%' THEN 'https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=900&q=75'

    -- Burgers / fast food
    WHEN name ILIKE '%бургер%' THEN 'https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%фрі%' THEN 'https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%нагетс%' THEN 'https://images.unsplash.com/photo-1565299507177-b0ac66763828?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%піца%' THEN 'https://images.unsplash.com/photo-1548365328-8b849e6f0d84?auto=format&fit=crop&w=900&q=75'

    -- Breakfast
    WHEN name ILIKE '%сирник%' THEN 'https://images.unsplash.com/photo-1567620905732-2d1ec7bb7445?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%вівсянк%' OR name ILIKE '%гранол%' OR name ILIKE '%йогурт%' THEN 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%яйц%' OR name ILIKE '%омлет%' OR name ILIKE '%скрембл%' OR name ILIKE '%шакшук%' THEN 'https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%круасан%' THEN 'https://images.unsplash.com/photo-1509440159598-8b1f3f58fdb5?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%тост%' THEN 'https://images.unsplash.com/photo-1484723091739-30a097e8f929?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%млинці%' OR name ILIKE '%французькі тости%' THEN 'https://images.unsplash.com/photo-1484723091739-30a097e8f929?auto=format&fit=crop&w=900&q=75'

    -- Desserts
    WHEN name ILIKE '%тіраміс%' THEN 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%чізкейк%' OR name ILIKE '%сирник львівський%' THEN 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%торт%' OR name ILIKE '%медовик%' OR name ILIKE '%наполеон%' THEN 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%фондан%' OR name ILIKE '%брауні%' OR name ILIKE '%шоколад%' THEN 'https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%морозиво%' THEN 'https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%штрудель%' OR name ILIKE '%пиріг%' THEN 'https://images.unsplash.com/photo-1519682577862-22b62b24e493?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%крем-брюле%' THEN 'https://images.unsplash.com/photo-1541783245831-57d6fb0926d3?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%макарон%' THEN 'https://images.unsplash.com/photo-1519869325930-281384150729?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%профітрол%' THEN 'https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?auto=format&fit=crop&w=900&q=75'

    -- Drinks
    WHEN name ILIKE '%еспресо%' OR name ILIKE '%американо%' OR name ILIKE '%капучино%' OR name ILIKE '%лате%' OR name ILIKE '%матча%' THEN 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%чай%' THEN 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%лимонад%' OR name ILIKE '%морс%' THEN 'https://images.unsplash.com/photo-1528823872057-9c018a7aa29f?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%фреш%' OR name ILIKE '%сік%' THEN 'https://images.unsplash.com/photo-1613478223719-2ab802602423?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%вода%' THEN 'https://images.unsplash.com/photo-1548839140-29a749e1cf4d?auto=format&fit=crop&w=900&q=75'

    -- Evening drinks
    WHEN name ILIKE '%мохіто%' OR name ILIKE '%маргарита%' OR name ILIKE '%апероль%' OR name ILIKE '%негроні%' OR name ILIKE '%олд фешн%' THEN 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%пиво%' THEN 'https://images.unsplash.com/photo-1436076863939-06870fe779c2?auto=format&fit=crop&w=900&q=75'
    WHEN name ILIKE '%вино%' THEN 'https://images.unsplash.com/photo-1510626176961-4b57d4fbad03?auto=format&fit=crop&w=900&q=75'

    ELSE 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=900&q=75'
END
WHERE image_url LIKE 'https://source.unsplash.com/%';

