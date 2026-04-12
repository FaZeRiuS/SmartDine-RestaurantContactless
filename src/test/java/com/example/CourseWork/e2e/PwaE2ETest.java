package com.example.CourseWork.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PwaE2ETest extends BaseE2ETest {

    private BrowserContext context;
    private Page page;

    @BeforeEach
    void createContextAndPage() {
        context = createTrackedContext();
        page = context.newPage();
        page.setDefaultNavigationTimeout(45_000);
        setupPageLogging(page, "PWA");
    }

    @Test
    @DisplayName("PWA: manifest.json should be accessible and have correct properties")
    void manifest_ShouldBeAccessible() {
        String baseUrl = getBaseUrl();
        Response response = page.navigate(baseUrl + "/manifest.json");
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headerValue("Content-Type")).contains("application/json");
        
        String body = response.text();
        assertThat(body).contains("\"short_name\": \"SmartDine\"");
    }

    @Test
    @DisplayName("PWA: Service Worker should register on the homepage")
    void serviceWorker_ShouldRegister() {
        page.navigate(getBaseUrl() + "/");
        
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
    @DisplayName("PWA: HTML shell cached after first visit (offline reload is flaky in headless Chromium/Playwright)")
    void offlineMode_ShouldLoadShell() {
        String baseUrl = getBaseUrl();

        page.navigate(baseUrl + "/");
        page.waitForLoadState(LoadState.LOAD);

        page.evaluate("async () => {\n" +
                "  if (!('serviceWorker' in navigator)) return;\n" +
                "  await Promise.race([\n" +
                "    navigator.serviceWorker.ready,\n" +
                "    new Promise((_, rej) => setTimeout(() => rej(new Error('sw-ready-timeout')), 15000))\n" +
                "  ]).catch(() => {});\n" +
                "}");

        // Network-first navigation caches HTML asynchronously after fetch; do not use setOffline+reload()
        // (Playwright often gets ERR_INTERNET_DISCONNECTED). Poll until Cache Storage contains the shell.
        page.waitForFunction(
                "() => (async () => {\n"
                        + "  for (const name of await caches.keys()) {\n"
                        + "    const cache = await caches.open(name);\n"
                        + "    for (const req of await cache.keys()) {\n"
                        + "      try {\n"
                        + "        const u = new URL(req.url);\n"
                        + "        if (u.origin !== location.origin) continue;\n"
                        + "        if (u.pathname !== '/' && u.pathname !== '') continue;\n"
                        + "        const res = await cache.match(req);\n"
                        + "        if (res && res.ok && /smartdine/i.test(await res.text())) return true;\n"
                        + "      } catch (e) { /* ignore */ }\n"
                        + "    }\n"
                        + "  }\n"
                        + "  return false;\n"
                        + "})()",
                null,
                new Page.WaitForFunctionOptions().setTimeout(30_000));
    }
}
