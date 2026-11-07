package com.example.campusfind.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CampusFindPlaywrightE2ETest {
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
    void memberCanRegisterPostAReportAndFindItWithTheAssistant() {
        String baseUrl = "http://127.0.0.1:" + port;
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "playwright-" + unique + "@example.edu";
        String itemName = "Blue bottle " + unique;
        Page page = browser.newPage();

        try {
            page.navigate(baseUrl + "/register");
            page.locator("#displayName").fill("Browser Tester");
            page.locator("#email").fill(email);
            page.locator("#password").fill("CampusFind-QA-123");
            page.locator("#confirmPassword").fill("CampusFind-QA-123");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Create account")).click();
            assertTrue(page.url().contains("/login?registered"), "Registration should reach the login confirmation");

            page.locator("#username").fill(email);
            page.locator("#password").fill("CampusFind-QA-123");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Log in")).click();
            assertTrue(page.url().contains("/account"), "The new member should be signed in");

            page.navigate(baseUrl + "/reports/new?type=FOUND");
            page.locator("#itemName").fill(itemName);
            page.locator("#category").selectOption("WATER_BOTTLES");
            page.locator("#description").fill("Blue reusable water bottle for automated browser testing.");
            page.locator("#lastSeenOrFoundAt").fill(LocalDate.now().toString());
            page.locator("#location").fill("Library");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Post report")).click();
            assertTrue(page.locator("h1").textContent().contains(itemName), "The report detail page should show the new report");

            page.navigate(baseUrl + "/assistant");
            page.locator("#question").fill("Where is the " + itemName + " at the library?");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Search with assistant")).click();
            page.waitForSelector("[data-testid='assistant-answer']",
                    new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
            assertTrue(page.locator("[data-testid='assistant-answer']").isVisible(), "The assistant should return an answer");
            assertTrue(page.locator("[data-testid='agent-report']").count() > 0, "The assistant should display the created report as a possible match");
            assertTrue(page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(itemName)).isVisible(), "The suggested report should link to its detail page");
            assertTrue(page.locator("[data-testid='assistant-status']").textContent().contains("AI_API_KEY"),
                    "The assistant should explain that a model key is needed for AI-generated responses");
        } finally {
            page.close();
        }
    }

    @Test
    void twoMembersCanInteractThroughMatchAlertsAndPrivateMessages() {
        String baseUrl = "http://127.0.0.1:" + port;
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String emailA = "poster-a-" + unique + "@example.edu";
        String emailB = "finder-b-" + unique + "@example.edu";
        String itemName = "Scientific Calculator " + unique;

        BrowserContext contextA = browser.newContext();
        BrowserContext contextB = browser.newContext();
        Page pageA = contextA.newPage();
        Page pageB = contextB.newPage();

        try {
            // Member A registers and posts LOST report
            pageA.navigate(baseUrl + "/register");
            pageA.locator("#displayName").fill("Member A");
            pageA.locator("#email").fill(emailA);
            pageA.locator("#password").fill("CampusFind-QA-123");
            pageA.locator("#confirmPassword").fill("CampusFind-QA-123");
            pageA.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Create account")).click();
            assertTrue(pageA.url().contains("/login?registered"), "Member A should be redirected to login");

            pageA.locator("#username").fill(emailA);
            pageA.locator("#password").fill("CampusFind-QA-123");
            pageA.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Log in")).click();
            assertTrue(pageA.url().contains("/account"), "Member A should be logged in");

            pageA.navigate(baseUrl + "/reports/new?type=LOST");
            pageA.locator("#itemName").fill(itemName);
            pageA.locator("#category").selectOption("ELECTRONICS");
            pageA.locator("#description").fill("Silver Casio scientific calculator left in the math lab.");
            pageA.locator("#lastSeenOrFoundAt").fill(LocalDate.now().toString());
            pageA.locator("#location").fill("Math Lab");
            pageA.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Post report")).click();
            assertTrue(pageA.locator("h1").textContent().contains(itemName));

            // Member B registers in separate isolated browser session and posts matching FOUND report
            pageB.navigate(baseUrl + "/register");
            pageB.locator("#displayName").fill("Member B");
            pageB.locator("#email").fill(emailB);
            pageB.locator("#password").fill("CampusFind-QA-123");
            pageB.locator("#confirmPassword").fill("CampusFind-QA-123");
            pageB.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Create account")).click();
            assertTrue(pageB.url().contains("/login?registered"));

            pageB.locator("#username").fill(emailB);
            pageB.locator("#password").fill("CampusFind-QA-123");
            pageB.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Log in")).click();
            assertTrue(pageB.url().contains("/account"), "Member B should be logged in");

            pageB.navigate(baseUrl + "/reports/new?type=FOUND");
            pageB.locator("#itemName").fill(itemName);
            pageB.locator("#category").selectOption("ELECTRONICS");
            pageB.locator("#description").fill("Found a Casio calculator on desk 3 in math lab.");
            pageB.locator("#lastSeenOrFoundAt").fill(LocalDate.now().toString());
            pageB.locator("#location").fill("Math Lab");
            pageB.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Post report")).click();
            assertTrue(pageB.locator("h1").textContent().contains(itemName));

            // Member A checks Match Alerts
            pageA.navigate(baseUrl + "/notifications");
            pageA.waitForSelector(".alert-row");
            assertTrue(pageA.content().contains("Possible match for"), "Member A should receive a high-score match alert");
            assertTrue(pageA.content().contains(itemName));

            // Member A clicks the alert, redirecting to Member B's Found report
            pageA.locator(".alert-row").first().click();
            pageA.waitForSelector(".detail-panel");
            assertTrue(pageA.url().contains("/reports/"), "Alert click should navigate to the found report");
            assertTrue(pageA.content().contains("Contact the poster privately"));
            assertFalse(pageA.content().contains(emailB), "Member B's email address must never be displayed on public listings");

            // Member A initiates a private conversation
            pageA.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("✉ Contact the poster privately")).click();
            pageA.waitForSelector("#body");
            assertTrue(pageA.url().contains("/contact"));
            pageA.locator("#body").fill("Hi, I lost my Casio calculator. Does it have my initials on the back cover?");
            pageA.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Send private message →")).click();
            pageA.waitForSelector(".message-item");
            assertTrue(pageA.url().contains("/inbox/"), "Should navigate to private conversation thread");
            assertTrue(pageA.content().contains("Does it have my initials on the back cover?"));
            assertFalse(pageA.content().contains(emailB), "Email addresses must remain hidden in conversation threads");

            // Member B checks inbox and sends reply
            pageB.navigate(baseUrl + "/inbox");
            pageB.waitForSelector(".inbox-row");
            assertTrue(pageB.content().contains("Member A"), "Inbox should show thread from Member A");
            pageB.locator(".inbox-row").first().click();
            pageB.waitForSelector(".message-item");
            assertTrue(pageB.content().contains("Does it have my initials on the back cover?"));
            assertFalse(pageB.content().contains(emailA), "Member A's email must not be exposed to Member B");

            pageB.locator("#body").fill("Yes, it has initials SP on the back! I will leave it at reception.");
            pageB.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Send reply →")).click();
            pageB.waitForSelector("text=Yes, it has initials SP on the back!");
            assertTrue(pageB.content().contains("Yes, it has initials SP on the back!"));

            // Member A refreshes and sees the reply
            pageA.reload();
            pageA.waitForSelector("text=Yes, it has initials SP on the back!");
            assertTrue(pageA.content().contains("Yes, it has initials SP on the back!"), "Member A should see Member B's reply");
        } finally {
            pageA.close();
            pageB.close();
            contextA.close();
            contextB.close();
        }
    }

    @Test
    void memberCanSaveAndResolveTheirOwnReport() {
        String baseUrl = "http://127.0.0.1:" + port;
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "owner-" + unique + "@example.edu";
        String itemName = "Black Umbrella " + unique;
        Page page = browser.newPage();

        try {
            // Register & login
            page.navigate(baseUrl + "/register");
            page.locator("#displayName").fill("Umbrella Owner");
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

            // Post Lost report
            page.navigate(baseUrl + "/reports/new?type=LOST");
            page.locator("#itemName").fill(itemName);
            page.locator("#category").selectOption("BAGS");
            page.locator("#description").fill("Black folding umbrella left in cafeteria.");
            page.locator("#lastSeenOrFoundAt").fill(LocalDate.now().toString());
            page.locator("#location").fill("Cafeteria");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Post report")).click();
            assertTrue(page.locator("h1").textContent().contains(itemName));
            String reportUrl = page.url();

            // Bookmark the report
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("♡ Save report")).click();
            page.waitForSelector("button:has-text('♥ Saved')");
            assertTrue(page.url().contains("?saved"));

            // Check bookmarks page
            page.navigate(baseUrl + "/saved");
            assertTrue(page.content().contains(itemName), "The saved page should display bookmarked reports");

            // Mark resolved
            page.navigate(reportUrl);
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("✓ Mark as resolved")).click();
            page.waitForSelector(".status-dot.resolved");
            assertTrue(page.content().contains("Report marked as resolved."));
            assertTrue(page.locator(".status-dot").textContent().contains("Resolved"));
        } finally {
            page.close();
        }
    }

    @Test
    void memberCanPostReportWithImageAndSeeImageOnDetailAndBrowsePages() throws Exception {
        String baseUrl = "http://127.0.0.1:" + port;
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "photo-test-" + unique + "@example.edu";
        String itemName = "Smart Watch " + unique;
        Page page = browser.newPage();

        try {
            // Register & login
            page.navigate(baseUrl + "/register");
            page.locator("#displayName").fill("Photo Tester");
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

            // Prepare a temporary test image file (valid 1x1 transparent PNG)
            java.nio.file.Path tempImage = java.nio.file.Files.createTempFile("campusfind_test_", ".png");
            byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
                0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08,
                0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89, 0x00, 0x00, 0x00,
                0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49,
                0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
            };
            java.nio.file.Files.write(tempImage, pngBytes);

            // Navigate to new report form
            page.navigate(baseUrl + "/reports/new?type=FOUND");
            page.locator("#itemName").fill(itemName);
            page.locator("#category").selectOption("ELECTRONICS");
            page.locator("#description").fill("Silver smart watch with silicone band found near sports ground.");
            page.locator("#lastSeenOrFoundAt").fill(LocalDate.now().toString());
            page.locator("#location").fill("Sports Complex");

            // Attach image
            page.setInputFiles("#image", tempImage);

            // Post report
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Post report")).click();

            // Assert detail page contains image
            assertTrue(page.locator("h1").textContent().contains(itemName));
            assertTrue(page.locator(".detail-photo").isVisible(), "The uploaded image should be displayed on detail page");
            String imgSrc = page.locator(".detail-photo").getAttribute("src");
            assertTrue(imgSrc != null && imgSrc.startsWith("/uploads/"), "Image source should point to /uploads/");

            // Navigate to reports list and verify photo badge / card
            page.navigate(baseUrl + "/reports?q=" + unique);
            assertTrue(page.locator(".photo-badge").isVisible(), "Browse page should show a photo badge on the card");

            // Clean up temp image
            java.nio.file.Files.deleteIfExists(tempImage);
        } finally {
            page.close();
        }
    }
}
