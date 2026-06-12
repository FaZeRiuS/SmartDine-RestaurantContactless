package com.example.CourseWork.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityAndResilienceE2ETest extends BaseE2ETest {

    private BrowserContext context;
    private Page page;

    @BeforeEach
    void createContextAndPage() {
        context = createTrackedContext(true);
        page = context.newPage();
        setupPageLogging(page, "SECURITY-RESILIENCE");
    }

    @Test
    @DisplayName("Security: Clickjacking mitigation headers should be served on root path")
    void clickjackingHeaders_ShouldBePresent() {
        String baseUrl = getBaseUrl();
        Response response = page.navigate(baseUrl + "/");
        assertThat(response.status()).isEqualTo(200);

        // Verify X-Frame-Options is DENY
        String xfo = response.headerValue("X-Frame-Options");
        assertThat(xfo).isEqualTo("DENY");

        // Verify Content-Security-Policy contains frame-ancestors 'none'
        String csp = response.headerValue("Content-Security-Policy");
        assertThat(csp).isNotNull().contains("frame-ancestors 'none'");
    }

    @Test
    @DisplayName("SSE: Offline Resilience should trigger UI sync when reconnecting")
    void sseOfflineResilience_ShouldSyncUiOnReconnect() {
        String baseUrl = getBaseUrl();

        // 1. Load home page & wait for connection
        loginAsGuest(page);
        page.navigate(baseUrl + "/");
        page.waitForLoadState(LoadState.LOAD);
        waitForSseConnection(page);

        // 2. Inject spy to check if UI is refreshed upon reconnection
        page.evaluate("() => {\n" +
                "  window.refreshUiCalled = false;\n" +
                "  const original = window.refreshUiAfterReloadNotification;\n" +
                "  window.refreshUiAfterReloadNotification = function() {\n" +
                "    window.refreshUiCalled = true;\n" +
                "    if (typeof original === 'function') {\n" +
                "      original();\n" +
                "    }\n" +
                "  };\n" +
                "}");

        // 3. Put context offline (this triggers EventSource.onerror, closing it and starting scheduleReconnect)
        System.out.println("TEST LOG: Simulating offline state...");
        context.setOffline(true);
        page.evaluate("if (window.eventSource) window.eventSource.dispatchEvent(new Event('error'));");
        
        // Wait for connection to transition to closed (sseConnected turns false)
        page.waitForFunction("() => window.sseConnected === false", 
            null, new Page.WaitForFunctionOptions().setTimeout(10000));

        // 4. Put context back online (manual reconnection will trigger startSseConnection -> connected -> wasDisconnected -> refreshUiAfterReloadNotification)
        System.out.println("TEST LOG: Simulating online state...");
        context.setOffline(false);

        // 5. Wait for sseConnected to turn true and spy flag to become true
        page.waitForFunction("() => window.sseConnected === true && window.refreshUiCalled === true", 
            null, new Page.WaitForFunctionOptions().setTimeout(15000));

        System.out.println("TEST LOG: Reconnection verified successfully, UI synced!");
    }
}
