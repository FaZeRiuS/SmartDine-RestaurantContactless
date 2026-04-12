package com.example.CourseWork.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "dev" })
class StaffJourneyE2ETest {

    @LocalServerPort
    private int port;

    private static Playwright playwright;
    private static Browser browser;

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

    @Test
    @DisplayName("Staff Journey: Should process order and update guest real-time via WebSockets")
    @SuppressWarnings("null")
    void orderProcessing_ShouldUpdateGuestRealTime() {
        String baseUrl = "http://localhost:" + port;

        // --- GUEST FLOW (Context 1) ---
        BrowserContext guestContext = browser.newContext();
        Page guestPage = guestContext.newPage();

        // Debugging logs
        guestPage.onConsoleMessage(msg -> System.out.println("GUEST LOG: " + msg.text()));
        guestPage.onPageError(err -> System.err.println("GUEST ERROR: " + err));

        // 1. Guest adds "Beef Steak" to cart
        guestPage.navigate(baseUrl + "/menu");
        guestPage.waitForLoadState(LoadState.LOAD);

        Locator beefCard = guestPage.locator(".dish-card").filter(new Locator.FilterOptions().setHasText("Beef Steak"))
                .first();
        beefCard.locator(".add-to-cart-btn").click();

        // Wait for confirmation toast to ensure item is added to DB
        assertThat(guestPage.locator("#toastContainer")).containsText("додано");

        // 2. Guest goes to cart and confirms order
        guestPage.navigate(baseUrl + "/cart");
        guestPage.waitForLoadState(LoadState.LOAD);

        // Wait for cart to load items (which makes the button visible)
        Locator confirmBtn = guestPage.locator("#confirmOrderBtn");
        assertThat(confirmBtn).isVisible();
        confirmBtn.click();

        // 3. Confirm order is now in "NEW" status
        Locator statusBadge = guestPage.locator("#activeOrderStatus");
        assertThat(statusBadge).containsText("Нове");

        // Extract Order ID for Staff to find it easily
        String orderIdText = guestPage.locator("#activeOrderId").textContent();
        System.out.println("TEST LOG: Guest placed Order #" + orderIdText);

        // --- STAFF FLOW (Context 2) ---
        BrowserContext staffContext = browser.newContext();
        Page staffPage = staffContext.newPage();

        // 1. Staff "Logins" via Backdoor
        staffPage.navigate(baseUrl + "/api/test/auth/login-as-staff");
        assertThat(staffPage.locator("body")).containsText("Successfully logged in");

        // 2. Staff goes to orders board
        staffPage.navigate(baseUrl + "/staff/orders");
        staffPage.waitForLoadState(LoadState.LOAD);

        // 3. Staff finds the order and clicks "Start Cooking"
        // The label in staff.js is "🔥 Готувати"
        Locator orderCard = staffPage.locator(".order-card")
                .filter(new Locator.FilterOptions().setHasText("#" + orderIdText)).first();
        assertThat(orderCard).isVisible();

        Locator processBtn = orderCard.locator("button")
                .filter(new Locator.FilterOptions().setHasText("Готувати"));
        processBtn.click();

        // Verify staff sees success
        Locator toast = staffPage.locator("#toastContainer");
        assertThat(toast).containsText("Готується");

        // --- REAL-TIME VERIFICATION (Back to Guest Context) ---

        // 4. Guest page should update AUTOMATICALLY via WebSockets
        // We wait for the specific toast notification first
        assertThat(guestPage.locator("#toastContainer")).containsText("почали готувати",
                new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(10000));

        // Then verify the badge update
        assertThat(statusBadge).containsText("Готується");

        // Cleanup
        staffContext.close();
        guestContext.close();
    }
}
