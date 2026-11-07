package com.example.campusfind.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"campusfind.ai.api-key=demo"})
class CampusFindPlaywrightAgentE2ETest {
    private static Playwright playwright;
    private static Browser browser;

    @Value("${local.server.port}")
    private int port;

    @BeforeAll
    static void startChromium() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void stopChromium() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @Test
    void memberCanUseAgenticAiAssistantInBrowser() {
        String baseUrl = "http://127.0.0.1:" + port;
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "ai-tester-" + unique + "@example.edu";
        String itemName = "Purple Flask " + unique;
        Page page = browser.newPage();

        try {
            // Register and login
            page.navigate(baseUrl + "/register");
            page.locator("#displayName").fill("AI Tester");
            page.locator("#email").fill(email);
            page.locator("#password").fill("CampusFind-QA-123");
            page.locator("#confirmPassword").fill("CampusFind-QA-123");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Create account")).click();
            assertTrue(page.url().contains("/login?registered"));

            page.locator("#username").fill(email);
            page.locator("#password").fill("CampusFind-QA-123");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Log in")).click();

            // Post Found report
            page.navigate(baseUrl + "/reports/new?type=FOUND");
            page.locator("#itemName").fill(itemName);
            page.locator("#category").selectOption("WATER_BOTTLES");
            page.locator("#description").fill("Purple insulated flask found by the chemistry building.");
            page.locator("#lastSeenOrFoundAt").fill(LocalDate.now().toString());
            page.locator("#location").fill("Chemistry Building");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Post report")).click();
            assertTrue(page.locator("h1").textContent().contains(itemName));

            // Use the AI assistant in the browser
            page.navigate(baseUrl + "/assistant");
            page.locator("#question").fill("I lost my " + itemName + " near the chemistry building");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Search with assistant")).click();

            page.waitForSelector("[data-testid='assistant-answer']",
                    new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
            page.waitForSelector("[data-testid='agent-report']",
                    new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

            assertTrue(page.locator("[data-testid='assistant-answer']").isVisible(),
                    "The agent assistant should return a response");
            assertTrue(page.content().contains("AI-assisted search"),
                    "The UI should badge the response as AI-assisted search");
            assertTrue(page.locator("[data-testid='assistant-answer']").textContent().contains("search_reports"),
                    "The answer should reflect the tool execution");
            assertTrue(page.locator("[data-testid='agent-report']").count() > 0,
                    "The matching report should be listed in the UI");
            assertTrue(page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(itemName)).isVisible(),
                    "The report should link to the detail page");

            // Click the report link to ensure navigation works
            page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(itemName)).click();
            page.waitForSelector(".detail-panel");
            assertTrue(page.url().contains("/reports/"));
            assertTrue(page.locator("h1").textContent().contains(itemName));
        } finally {
            page.close();
        }
    }
}
