package com.example.CourseWork.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "dev"})
class CustomerJourneyE2ETest extends BaseE2ETest {

    private Page page;

    @BeforeEach
    void createContextAndPage() {
        BrowserContext context = createTrackedContext();
        page = context.newPage();
        setupPageLogging(page, "CUSTOMER");
    }

    @Test
    @DisplayName("Customer Journey: Should browse menu, add to cart, and verify cart content")
    void fullCustomerJourney_ShouldSucceed() {
        String baseUrl = getBaseUrl();

        // 1. Guest Logins and navigates to Menu
        loginAsGuest(page);
        int mainMenuId = getMenuIdByName(page, "Main Menu");
        page.navigate(baseUrl + "/menu?id=" + mainMenuId);
        page.waitForLoadState(LoadState.LOAD);
        waitForMenuDishes(page);

        // Verify we are on the menu page
        assertThat(page).hasTitle(org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> java.util.regex.Pattern.compile(".*Меню.*", java.util.regex.Pattern.CASE_INSENSITIVE)));
        
        // 2. Find "Grilled Salmon" and add to cart
        Locator salmonCard = page.locator(".dish-card").filter(new Locator.FilterOptions().setHasText("Grilled Salmon")).first();
        assertThat(salmonCard).isVisible();
        
        // --- SSE Sync: Ensure we are connected before acting (good practice even if not using it for logic here) ---
        waitForSseConnection(page);

        Locator addToCartBtn = salmonCard.locator(".add-to-cart-btn");
        addToCartBtn.click();
        
        // --- Stabilization: wait a bit after click ---
        page.waitForTimeout(1000);

        // 3. Wait for Success Toast
        Locator toast = page.locator("#toastContainer");
        assertThat(toast).containsText("додано");

        // 4. Navigate to Cart
        page.navigate(baseUrl + "/cart");
        page.waitForLoadState(LoadState.LOAD);
        
        // Wait for cart items to render
        page.waitForSelector("#cartItems .cart-item", new Page.WaitForSelectorOptions().setTimeout(5000));

        // Verify we are on the cart page
        assertThat(page).hasTitle(org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> java.util.regex.Pattern.compile(".*Кошик.*", java.util.regex.Pattern.CASE_INSENSITIVE)));

        // 5. Verify "Grilled Salmon" is in the cart
        // Using hasCount automatically waits for the element to appear
        Locator cartItem = page.locator("#cartItems").locator(".cart-item").filter(new Locator.FilterOptions().setHasText("Grilled Salmon"));
        assertThat(cartItem).isVisible();
        assertThat(cartItem).hasCount(1);

        // 6. Verify total price (Grilled Salmon is 24.99 in DataSeeder)
        Locator totalPrice = page.locator("#cartTotalPrice");
        assertThat(totalPrice).containsText("24.99");

        // 7. Verify Confirm Order button is present
        Locator confirmBtn = page.locator("#confirmOrderBtn");
        assertThat(confirmBtn).isVisible();
    }
}
