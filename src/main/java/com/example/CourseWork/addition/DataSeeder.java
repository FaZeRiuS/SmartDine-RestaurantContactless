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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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
                                                        24.99f, true, Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1485921325833-c519f76c4927?auto=format&fit=crop&w=600&q=75",
                                                        "seafood", "grilled", "healthy"),
                                        createDish("Beef Steak", "Juicy pepper crusted steak", 24.99f, true,
                                                        Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=600&q=75",
                                                        "meat", "grilled", "main"),
                                        createDish("Chicken Alfredo", "Creamy pasta with grilled chicken", 18.99f, true,
                                                        Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1473093226795-af9932fe5856?auto=format&fit=crop&w=600&q=75",
                                                        "pasta", "chicken", "main"),
                                        createDish("Vegetable Stir Fry", "Fresh seasonal vegetables with soy sauce",
                                                        15.99f, true, Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=600&q=75",
                                                        "vegan", "healthy", "main"),
                                        createDish("Fish and Chips", "Crispy cod with golden fries", 19.99f, true,
                                                        Arrays.asList(mainMenu),
                                                        "https://images.unsplash.com/photo-1706711053549-f52f73a8960c?&auto=format&fit=crop&w=600&q=75",
                                                        "fish", "fried", "main"));

                        List<Dish> desserts = Arrays.asList(
                                        createDish("Chocolate Cake", "Rich chocolate cake with ganache", 8.99f, true,
                                                        Arrays.asList(dessertsMenu),
                                                        "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=600&q=75",
                                                        "sweet", "chocolate", "dessert"),
                                        createDish("Cheesecake", "Classic New York style cheesecake", 7.99f, true,
                                                        Arrays.asList(dessertsMenu),
                                                        "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=600&q=75",
                                                        "sweet", "dessert"),
                                        createDish("Tiramisu", "Authentic Italian tiramisu", 8.99f, true,
                                                        Arrays.asList(dessertsMenu),
                                                        "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=600&q=75",
                                                        "sweet", "dessert"),
                                        createDish("Ice Cream Sundae", "Triple scoop vanilla with chocolate sauce",
                                                        6.99f, true, Arrays.asList(dessertsMenu),
                                                        "https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?auto=format&fit=crop&w=600&q=75",
                                                        "sweet", "dessert", "cold"));

                        List<Dish> drinks = Arrays.asList(
                                        createDish("Fresh Orange Juice", "Freshly squeezed orange juice", 4.99f, true,
                                                        Arrays.asList(drinksMenu, breakfastMenu),
                                                        "https://images.unsplash.com/photo-1613478223719-2ab802602423?auto=format&fit=crop&w=600&q=75",
                                                        "drink", "cold", "healthy", "fruit"),
                                        createDish("Iced Tea", "Refreshing iced tea with lemon", 3.99f, true,
                                                        Arrays.asList(drinksMenu),
                                                        "https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=600&q=75",
                                                        "drink", "cold", "tea"),
                                        createDish("Coffee", "Freshly brewed coffee", 3.49f, true,
                                                        Arrays.asList(drinksMenu, breakfastMenu),
                                                        "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=600&q=75",
                                                        "drink", "hot", "coffee"),
                                        createDish("Mineral Water", "Sparkling mineral water", 2.99f, true,
                                                        Arrays.asList(drinksMenu),
                                                        "https://images.unsplash.com/photo-1548839140-29a749e1cf4d?auto=format&fit=crop&w=600&q=75",
                                                        "drink", "cold", "water"));

                        List<Dish> breakfastDishes = Arrays.asList(
                                        createDish("Pancakes", "Fluffy pancakes with maple syrup", 12.91f, true,
                                                        Arrays.asList(breakfastMenu),
                                                        "https://images.unsplash.com/photo-1567620905732-2d1ec7bb7445?auto=format&fit=crop&w=600&q=75",
                                                        "breakfast", "sweet", "hot"),
                                        createDish("Omelette", "Three egg omelette with cheese and herbs", 11.99f, true,
                                                        Arrays.asList(breakfastMenu),
                                                        "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=600&q=75",
                                                        "breakfast", "eggs", "hot"),
                                        createDish("Avocado Toast", "Sourdough toast with mashed avocado", 12.99f, true,
                                                        Arrays.asList(breakfastMenu),
                                                        "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=600&q=75",
                                                        "breakfast", "healthy", "vegan"));

                        List<Dish> eveningDishes = Arrays.asList(
                                        createDish("Mojito", "Classic Cuban highball", 12.99f, true,
                                                        Arrays.asList(eveningSpecialsMenu, drinksMenu),
                                                        "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=600&q=75",
                                                        "cocktail", "drink", "cold", "alcohol"),
                                        createDish("Margarita", "Tequila, triple sec, lime juice", 13.99f, true,
                                                        Arrays.asList(eveningSpecialsMenu, drinksMenu),
                                                        "https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?auto=format&fit=crop&w=600&q=75",
                                                        "cocktail", "drink", "cold", "alcohol"),
                                        createDish("Ribeye Steak", "Premium cut ribeye with roasted potatoes", 34.99f,
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
                java.util.Map<String, String> workingUrls = new java.util.HashMap<>();
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

                workingUrls.forEach(this::updateImageUrl);
        }

        private void updateImageUrl(String dishName, String newUrl) {
                dishRepository.findAll().stream()
                                .filter(d -> d.getName().trim().equalsIgnoreCase(dishName.trim()))
                                .findFirst()
                                .ifPresent(d -> {
                                        // Update if URL is empty, null, or differs from the new working one
                                        if (d.getImageUrl() == null || d.getImageUrl().isEmpty()
                                                        || !d.getImageUrl().equals(newUrl)) {
                                                log.info(">>> SEEDER: Updating image for dish: {} -> {}", d.getName(),
                                                                newUrl);
                                                d.setImageUrl(newUrl);
                                                dishRepository.save(d);
                                        }
                                });
        }

        private void seedOrders(List<Dish> mainDishes, List<Dish> desserts, List<Dish> drinks) {
                // Create a couple of orders for test users to boost dish popularity
                Order order1 = new Order();
                order1.setUserId("dummy-id-1");
                order1.setCreatedAt(LocalDateTime.now().minusDays(1));
                order1.setStatus(OrderStatus.COMPLETED);

                OrderItem item1 = new OrderItem();
                item1.setDish(mainDishes.get(1)); // Beef Steak
                item1.setQuantity(2);
                item1.setOrder(order1);

                OrderItem item2 = new OrderItem();
                item2.setDish(desserts.get(0)); // Chocolate Cake
                item2.setQuantity(1);
                item2.setOrder(order1);

                order1.setItems(Arrays.asList(item1, item2));
                order1.setTotalPrice(mainDishes.get(1).getPrice() * 2 + desserts.get(0).getPrice());
                orderRepository.save(order1);

                // order2 makes Beef Steak even more popular
                Order order2 = new Order();
                order2.setUserId("dummy-id-2");
                order2.setCreatedAt(LocalDateTime.now().minusHours(2));
                order2.setStatus(OrderStatus.COMPLETED);

                OrderItem item3 = new OrderItem();
                item3.setDish(mainDishes.get(1)); // Beef Steak
                item3.setQuantity(1);
                item3.setOrder(order2);

                OrderItem item4 = new OrderItem();
                item4.setDish(mainDishes.get(0)); // Grilled Salmon
                item4.setQuantity(1);
                item4.setOrder(order2);

                order2.setItems(Arrays.asList(item3, item4));
                order2.setTotalPrice(mainDishes.get(1).getPrice() + mainDishes.get(0).getPrice());
                orderRepository.save(order2);
        }

        private Dish createDish(String name, String description, float price, boolean isAvailable, List<Menu> menus,
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