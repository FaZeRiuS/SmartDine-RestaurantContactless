package com.example.CourseWork.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "dev" })
class StaffJourneyE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Staff Journey: Should process order and update guest real-time via SSE")
    @SuppressWarnings("null")
    void orderProcessing_ShouldUpdateGuestRealTime() {
        String baseUrl = getBaseUrl();

        // --- GUEST FLOW (Context 1) ---
        BrowserContext guestContext = createTrackedContext();
        Page guestPage = guestContext.newPage();

        // Setup logging using base helper
        setupPageLogging(guestPage, "GUEST");

        // 1. Guest adds "Beef Steak" to cart
        guestPage.navigate(baseUrl + "/menu");
        guestPage.waitForLoadState(LoadState.LOAD);
        
        // --- SSE Sync: Ensure Guest is connected before Staff triggers updates ---
        waitForSseConnection(guestPage);

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
        BrowserContext staffContext = createTrackedContext();
        Page staffPage = staffContext.newPage();
        setupPageLogging(staffPage, "STAFF");

        // 1. Staff "Logins" via Backdoor helper
        loginAsStaff(staffPage);
        assertThat(staffPage.locator("body")).containsText("Successfully logged in");

        // 2. Staff goes to orders board
        staffPage.navigate(baseUrl + "/staff/orders");
        staffPage.waitForLoadState(LoadState.LOAD);
        
        // Staff also listens via SSE for the board to refresh
        waitForSseConnection(staffPage);

        // 3. Staff finds the order and clicks "Start Cooking"
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

        // 4. Guest page should update AUTOMATICALLY via SSE
        // We wait for the specific toast notification first
        assertThat(guestPage.locator("#toastContainer")).containsText("почали готувати",
                new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(10000));

        // Then verify the badge update
        assertThat(statusBadge).containsText("Готується");

        // Cleanup is handled automatically by BaseE2ETest.cleanupContexts()
    }
}
