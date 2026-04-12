package com.example.CourseWork.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PwaE2ETest {

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
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("PWA: manifest.json should be accessible and have correct properties")
    void manifest_ShouldBeAccessible() {
        Response response = page.navigate("http://localhost:" + port + "/manifest.json");
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headerValue("Content-Type")).contains("application/json");
        
        String body = response.text();
        assertThat(body).contains("\"short_name\": \"SmartDine\"");
    }

    @Test
    @DisplayName("PWA: Service Worker should register on the homepage")
    void serviceWorker_ShouldRegister() {
        page.navigate("http://localhost:" + port + "/");
        
        // Wait for registration logic in UI
        page.waitForLoadState(LoadState.LOAD);
        
        // Check service worker status via JS
        Object swStatus = page.evaluate("async () => {\n" +
                "  if ('serviceWorker' in navigator) {\n" +
                "    const registrations = await navigator.serviceWorker.getRegistrations();\n" +
                "    return registrations.length > 0;\n" +
                "  }\n" +
                "  return false;\n" +
                "}");
        
        assertThat((Boolean) swStatus).isTrue();
    }

    @Test
    @DisplayName("PWA: Shell should be available offline after first visit")
    void offlineMode_ShouldLoadShell() {
        // 1. Initial load to populate cache
        page.navigate("http://localhost:" + port + "/");
        page.waitForLoadState(LoadState.LOAD);

        // Wait for service worker to take control
        page.evaluate("async () => {\n" +
                "  if ('serviceWorker' in navigator) {\n" +
                "    await navigator.serviceWorker.ready;\n" +
                "    // Small delay to ensure activate event finished and it controls the page\n" +
                "    await new Promise(r => setTimeout(r, 200));\n" +
                "  }\n" +
                "}");

        // 2. Go offline
        context.setOffline(true);

        // 3. Reload and verify page loads from CACHE
        // We use navigate instead of reload to be more explicit, 
        // and we expect it to work because of the SW.
        page.navigate("http://localhost:" + port + "/");
        page.waitForLoadState();

        assertThat(page.title()).containsIgnoringCase("SmartDine");
    }
}
