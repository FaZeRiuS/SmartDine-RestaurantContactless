package com.example.CourseWork.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ServiceWorkerPolicy;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.web.server.LocalServerPort;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Hard cap so a stuck Playwright step cannot hang the Maven run indefinitely. */
@Timeout(value = 8, unit = TimeUnit.MINUTES)
public abstract class BaseE2ETest {

    @LocalServerPort
    protected int port;

    protected static Playwright playwright;
    protected static Browser browser;
    
    // Track contexts for automatic cleanup
    protected List<BrowserContext> contexts = new ArrayList<>();

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (Exception ignored) {
            // best-effort shutdown
        } finally {
            browser = null;
        }

        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception ignored) {
            // best-effort shutdown
        } finally {
            playwright = null;
        }
    }

    @AfterEach
    void cleanupContexts() {
        for (BrowserContext context : contexts) {
            try {
                context.close();
            } catch (Exception e) {
                // Ignore cleanup errors during teardown
            }
        }
        contexts.clear();
    }

    /**
     * @param blockServiceWorkers {@code true} for order/cart E2E — avoids SW from another test’s origin/port
     *         intercepting HTMX (flaky 403 / swapError in full Maven runs). PWA tests should pass {@code false}.
     */
    protected BrowserContext createTrackedContext(boolean blockServiceWorkers) {
        Browser.NewContextOptions opts = new Browser.NewContextOptions();
        if (blockServiceWorkers) {
            opts.setServiceWorkers(ServiceWorkerPolicy.BLOCK);
        }
        BrowserContext context = browser.newContext(opts);
        contexts.add(context);
        return context;
    }

    protected BrowserContext createTrackedContext() {
        return createTrackedContext(true);
    }

    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Waits for the SSE connection to be established and in OPEN state.
     * Uses window.sseConnected from sse.js (EventSource).
     */
    protected void waitForSseConnection(Page page) {
        // Wait for our custom flag which is set only after the 'connected' event is received
        page.waitForFunction("() => window.sseConnected === true", 
            null, new Page.WaitForFunctionOptions().setTimeout(15000));
    }

    /**
     * Uses the staff backdoor to bypass Keycloak login in test environments.
     */
    protected void loginAsStaff(Page page) {
        page.navigate(getBaseUrl() + "/api/test/auth/login-as-staff");
    }

    /**
     * Uses the guest backdoor for E2E testing.
     */
    protected void loginAsGuest(Page page) {
        page.navigate(getBaseUrl() + "/api/test/auth/login-as-guest");
        assertThat(page.locator("body")).containsText("Successfully logged in");
    }

    /**
     * Standard page logging setup for easier debugging of E2E failures.
     */
    protected void setupPageLogging(Page page, String prefix) {
        page.onConsoleMessage(msg -> System.out.println(prefix + " LOG: " + msg.text()));
        page.onPageError(err -> System.err.println(prefix + " ERROR: " + err));
    }
}
