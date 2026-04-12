package com.example.CourseWork.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "dev"})
class CustomerJourneyE2ETest {

    @LocalServerPort
    private int port;

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();
        
        // Log browser console for debugging
        page.onConsoleMessage(msg -> System.out.println("BROWSER LOG: " + msg.text()));
        page.onPageError(err -> System.err.println("BROWSER ERROR: " + err));
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("Customer Journey: Should browse menu, add to cart, and verify cart content")
    void fullCustomerJourney_ShouldSucceed() {
        String baseUrl = "http://localhost:" + port;

        // 1. Navigate to Menu
        page.navigate(baseUrl + "/menu");
        page.waitForLoadState(LoadState.LOAD);

        // Verify we are on the menu page
        assertThat(page).hasTitle(org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> java.util.regex.Pattern.compile(".*Меню.*", java.util.regex.Pattern.CASE_INSENSITIVE)));
        
        // 2. Find "Grilled Salmon" and add to cart
        Locator salmonCard = page.locator(".dish-card").filter(new Locator.FilterOptions().setHasText("Grilled Salmon")).first();
        assertThat(salmonCard).isVisible();
        
        Locator addToCartBtn = salmonCard.locator(".add-to-cart-btn");
        addToCartBtn.click();

        // 3. Wait for Success Toast
        Locator toast = page.locator("#toastContainer");
        assertThat(toast).containsText("додано");

        // 4. Navigate to Cart
        page.navigate(baseUrl + "/cart");
        page.waitForLoadState(LoadState.LOAD);

        // Verify we are on the cart page
        assertThat(page).hasTitle(org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> java.util.regex.Pattern.compile(".*Кошик.*", java.util.regex.Pattern.CASE_INSENSITIVE)));

        // 5. Verify "Grilled Salmon" is in the cart
        // Using hasCount automatically waits for the element to appear
        Locator cartItem = page.locator("#cartItems").locator(".cart-item").filter(new Locator.FilterOptions().setHasText("Grilled Salmon"));
        assertThat(cartItem).hasCount(1);

        // 6. Verify total price (Grilled Salmon is 24.99 in DataSeeder)
        Locator totalPrice = page.locator("#cartTotalPrice");
        assertThat(totalPrice).containsText("24.99");

        // 7. Verify Confirm Order button is present
        Locator confirmBtn = page.locator("#confirmOrderBtn");
        assertThat(confirmBtn).isVisible();
    }
}
