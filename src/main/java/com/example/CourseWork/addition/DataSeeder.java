package com.example.CourseWork.addition;

import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Menu;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.MenuRepository;
import com.example.CourseWork.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

@Component
@Profile("dev")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class DataSeeder {

        private final MenuRepository menuRepository;
        private final DishRepository dishRepository;
        private final OrderRepository orderRepository;

        @PostConstruct
        @SuppressWarnings("null")
        public void seedData() {
                if (menuRepository.count() == 0) {
                        Menu mainMenu = new Menu();
                        mainMenu.setName("Main Menu");
                        menuRepository.save(mainMenu);

                        Menu dessertsMenu = new Menu();
                        dessertsMenu.setName("Desserts");
                        menuRepository.save(dessertsMenu);

                        Menu drinksMenu = new Menu();
                        drinksMenu.setName("Drinks");
                        menuRepository.save(drinksMenu);

                        Menu breakfastMenu = new Menu();
                        breakfastMenu.setName("Breakfasts");
                        breakfastMenu.setStartTime(java.time.LocalTime.of(6, 0));
                        breakfastMenu.setEndTime(java.time.LocalTime.of(12, 0));
                        menuRepository.save(breakfastMenu);

                        Menu eveningSpecialsMenu = new Menu();
                        eveningSpecialsMenu.setName("Evening Specials");
                        eveningSpecialsMenu.setStartTime(java.time.LocalTime.of(18, 0));
                        eveningSpecialsMenu.setEndTime(java.time.LocalTime.of(23, 59));
                        menuRepository.save(eveningSpecialsMenu);

                        List<Dish> mainDishes = Arrays.asList(
                                        createDish("Grilled Salmon", "Fresh Atlantic salmon with lemon butter sauce",
                                                        new BigDecimal("24.99"), true, Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1485921325833-c519f76c4927?auto=format&fit=crop&w=600&q=75",
                                                        "seafood", "grilled", "healthy"),
                                        createDish("Beef Steak", "Juicy pepper crusted steak", new BigDecimal("24.99"), true,
                                                        Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=600&q=75",
                                                        "meat", "grilled", "main"),
                                        createDish("Chicken Alfredo", "Creamy pasta with grilled chicken", new BigDecimal("18.99"), true,
                                                        Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1473093226795-af9932fe5856?auto=format&fit=crop&w=600&q=75",
                                                        "pasta", "chicken", "main"),
                                        createDish("Vegetable Stir Fry", "Fresh seasonal vegetables with soy sauce",
                                                        new BigDecimal("15.99"), true, Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=600&q=75",
                                                        "vegan", "healthy", "main"),
                                        createDish("Fish and Chips", "Crispy cod with golden fries", new BigDecimal("19.99"), true,
                                                        Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1706711053549-f52f73a8960c?&auto=format&fit=crop&w=600&q=75",
                                                        "fish", "fried", "main"));

                        List<Dish> desserts = Arrays.asList(
                                        createDish("Chocolate Cake", "Rich chocolate cake with ganache", new BigDecimal("8.99"), true,
                                                        Arrays.asList(dessertsMenu),
                                                        "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=600&q=75",
                                                        "sweet", "chocolate", "dessert"),
                                        createDish("Cheesecake", "Classic New York style cheesecake", new BigDecimal("7.99"), true,
                                                        Arrays.asList(dessertsMenu),
                                                        "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=600&q=75",
                                                        "sweet", "dessert"),
                                        createDish("Tiramisu", "Authentic Italian tiramisu", new BigDecimal("8.99"), true,
                                                        Arrays.asList(dessertsMenu),
                                                        "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=600&q=75",
                                                        "sweet", "dessert"),
                                        createDish("Ice Cream Sundae", "Triple scoop vanilla with chocolate sauce",
                                                        new BigDecimal("6.99"), true, Arrays.asList(dessertsMenu),
                                                        "https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?auto=format&fit=crop&w=600&q=75",
                                                        "sweet", "dessert", "cold"));

                        List<Dish> drinks = Arrays.asList(
                                        createDish("Fresh Orange Juice", "Freshly squeezed orange juice", new BigDecimal("4.99"), true,
                                                        Arrays.asList(drinksMenu, breakfastMenu),
                                                        "https://images.unsplash.com/photo-1613478223719-2ab802602423?auto=format&fit=crop&w=600&q=75",
                                                        "drink", "cold", "healthy", "fruit"),
                                        createDish("Iced Tea", "Refreshing iced tea with lemon", new BigDecimal("3.99"), true,
                                                        Arrays.asList(drinksMenu),
                                                        "https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=600&q=75",
                                                        "drink", "cold", "tea"),
                                        createDish("Coffee", "Freshly brewed coffee", new BigDecimal("3.49"), true,
                                                        Arrays.asList(drinksMenu, breakfastMenu),
                                                        "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=600&q=75",
                                                        "drink", "hot", "coffee"),
                                        createDish("Mineral Water", "Sparkling mineral water", new BigDecimal("2.99"), true,
                                                        Arrays.asList(drinksMenu),
                                                        "https://images.unsplash.com/photo-1548839140-29a749e1cf4d?auto=format&fit=crop&w=600&q=75",
                                                        "drink", "cold", "water"));

                        List<Dish> breakfastDishes = Arrays.asList(
                                        createDish("Pancakes", "Fluffy pancakes with maple syrup", new BigDecimal("12.91"), true,
                                                        Arrays.asList(breakfastMenu),
                                                        "https://images.unsplash.com/photo-1567620905732-2d1ec7bb7445?auto=format&fit=crop&w=600&q=75",
                                                        "breakfast", "sweet", "hot"),
                                        createDish("Omelette", "Three egg omelette with cheese and herbs", new BigDecimal("11.99"), true,
                                                        Arrays.asList(breakfastMenu),
                                                        "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=600&q=75",
                                                        "breakfast", "eggs", "hot"),
                                        createDish("Avocado Toast", "Sourdough toast with mashed avocado", new BigDecimal("12.99"), true,
                                                        Arrays.asList(breakfastMenu),
                                                        "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=600&q=75",
                                                        "breakfast", "healthy", "vegan"));

                        List<Dish> eveningDishes = Arrays.asList(
                                        createDish("Mojito", "Classic Cuban highball", new BigDecimal("12.99"), true,
                                                        Arrays.asList(eveningSpecialsMenu, drinksMenu),
                                                        "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=600&q=75",
                                                        "cocktail", "drink", "cold", "alcohol"),
                                        createDish("Margarita", "Tequila, triple sec, lime juice", new BigDecimal("13.99"), true,
                                                        Arrays.asList(eveningSpecialsMenu, drinksMenu),
                                                        "https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?auto=format&fit=crop&w=600&q=75",
                                                        "cocktail", "drink", "cold", "alcohol"),
                                        createDish("Ribeye Steak", "Premium cut ribeye with roasted potatoes", new BigDecimal("34.99"),
                                                        true, Arrays.asList(eveningSpecialsMenu, mainMenu),
                                                        "https://images.unsplash.com/photo-1432139555190-58524dae6a55?auto=format&fit=crop&w=600&q=75",
                                                        "meat", "grilled", "premium", "main"));

                        log.info(">>> SEEDER: Database is empty. Seeding initial data...");
                        dishRepository.saveAll(mainDishes);
                        dishRepository.saveAll(desserts);
                        dishRepository.saveAll(drinks);
                        dishRepository.saveAll(breakfastDishes);
                        dishRepository.saveAll(eveningDishes);

                        seedOrders(mainDishes, desserts, drinks);
                        log.info(">>> SEEDER: Initial data seeded successfully.");
                } else {
                        log.info(">>> SEEDER: Database already contains data. Checking for image updates...");
                        // Update existing dishes with working image URLs
                        updateExistingDishes();
                        log.info(">>> SEEDER: Content checks completed.");
                }
        }

        private void updateExistingDishes() {
                // Map of dish names to their NEW working URLs
                Map<String, String> workingUrls = new HashMap<>();
                workingUrls.put("Grilled Salmon",
                                "https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Beef Steak",
                                "https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Chicken Alfredo",
                                "https://images.unsplash.com/photo-1473093226795-af9932fe5856?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Vegetable Stir Fry",
                                "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Fish and Chips",
                                "https://images.unsplash.com/photo-1706711053549-f52f73a8960c?q=80&w=2350&auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Chocolate Cake",
                                "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Cheesecake",
                                "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Tiramisu",
                                "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Ice Cream Sundae",
                                "https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Fresh Orange Juice",
                                "https://images.unsplash.com/photo-1613478223719-2ab802602423?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Iced Tea",
                                "https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Coffee",
                                "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Mineral Water",
                                "https://images.unsplash.com/photo-1548839140-29a749e1cf4d?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Pancakes",
                                "https://images.unsplash.com/photo-1567620905732-2d1ec7bb7445?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Omelette",
                                "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Avocado Toast",
                                "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Mojito",
                                "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Margarita",
                                "https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?auto=format&fit=crop&w=600&q=75");
                workingUrls.put("Ribeye Steak",
                                "https://images.unsplash.com/photo-1432139555190-58524dae6a55?auto=format&fit=crop&w=600&q=75");

                List<Dish> allDishes = dishRepository.findAll();
                Map<String, Dish> dishByName = new HashMap<>();
                for (Dish d : allDishes) {
                        if (d == null || d.getName() == null) continue;
                        dishByName.put(normalizeKey(d.getName()), d);
                }

                List<Dish> changed = new ArrayList<>();
                workingUrls.forEach((dishName, newUrl) -> {
                        Dish d = dishByName.get(normalizeKey(dishName));
                        if (d == null) return;
                        if (newUrl == null || newUrl.isEmpty()) return;
                        String current = d.getImageUrl();
                        if (current == null || current.isEmpty() || !current.equals(newUrl)) {
                                log.info(">>> SEEDER: Updating image for dish: {} -> {}", d.getName(), newUrl);
                                d.setImageUrl(newUrl);
                                changed.add(d);
                        }
                });

                if (!changed.isEmpty()) {
                        dishRepository.saveAll(changed);
                }
        }

        private static String normalizeKey(String s) {
                return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        }

        private void seedOrders(List<Dish> mainDishes, List<Dish> desserts, List<Dish> drinks) {
                // Create a couple of orders for test users to boost dish popularity
                Order order1 = new Order();
                order1.setUserId("dummy-id-1");
                order1.setCreatedAt(OffsetDateTime.now().minusDays(1));
                order1.setStatus(OrderStatus.COMPLETED);
                order1.setPaymentStatus(PaymentStatus.SUCCESS);

                OrderItem item1 = new OrderItem();
                item1.setDish(mainDishes.get(1)); // Beef Steak
                item1.setQuantity(2);
                item1.setOrder(order1);

                OrderItem item2 = new OrderItem();
                item2.setDish(desserts.get(0)); // Chocolate Cake
                item2.setQuantity(1);
                item2.setOrder(order1);

                order1.setItems(Arrays.asList(item1, item2));
                order1.setTotalPrice(
                        mainDishes.get(1).getPrice().multiply(BigDecimal.valueOf(2))
                                .add(desserts.get(0).getPrice())
                                .setScale(2, RoundingMode.HALF_UP)
                );
                orderRepository.save(order1);

                // order2 makes Beef Steak even more popular
                Order order2 = new Order();
                order2.setUserId("dummy-id-2");
                order2.setCreatedAt(OffsetDateTime.now().minusHours(2));
                order2.setStatus(OrderStatus.COMPLETED);
                order2.setPaymentStatus(PaymentStatus.SUCCESS);

                OrderItem item3 = new OrderItem();
                item3.setDish(mainDishes.get(1)); // Beef Steak
                item3.setQuantity(1);
                item3.setOrder(order2);

                OrderItem item4 = new OrderItem();
                item4.setDish(mainDishes.get(0)); // Grilled Salmon
                item4.setQuantity(1);
                item4.setOrder(order2);

                order2.setItems(Arrays.asList(item3, item4));
                order2.setTotalPrice(
                        mainDishes.get(1).getPrice()
                                .add(mainDishes.get(0).getPrice())
                                .setScale(2, RoundingMode.HALF_UP)
                );
                orderRepository.save(order2);
        }

        private Dish createDish(String name, String description, BigDecimal price, boolean isAvailable, List<Menu> menus,
                        String imageUrl, String... tags) {
                Dish dish = new Dish();
                dish.setName(name);
                dish.setDescription(description);
                dish.setPrice(price);
                dish.setIsAvailable(isAvailable);
                dish.setImageUrl(imageUrl);
                dish.setMenus(menus);
                dish.setTags(Arrays.asList(tags));
                return dish;
        }
}