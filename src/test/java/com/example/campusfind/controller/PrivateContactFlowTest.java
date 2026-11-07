package com.example.campusfind.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.campusfind.model.ContactMessage;
import com.example.campusfind.model.ContactThread;
import com.example.campusfind.model.MatchAlert;
import com.example.campusfind.model.ItemCategory;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportType;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.ContactMessageRepository;
import com.example.campusfind.repository.ContactThreadRepository;
import com.example.campusfind.repository.MatchAlertRepository;
import com.example.campusfind.repository.ReportRepository;
import com.example.campusfind.repository.SavedReportRepository;
import com.example.campusfind.repository.UserAccountRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PrivateContactFlowTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository accounts;
    @Autowired ReportRepository reports;
    @Autowired ContactThreadRepository threads;
    @Autowired ContactMessageRepository messages;
    @Autowired SavedReportRepository savedReports;
    @Autowired MatchAlertRepository alerts;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearData() {
        messages.deleteAll();
        threads.deleteAll();
        alerts.deleteAll();
        savedReports.deleteAll();
        reports.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void reportPosterAndFinderCanMessagePrivatelyWithoutExposingEmail() throws Exception {
        UserAccount poster = accounts.save(new UserAccount("Poster", "poster@example.edu", passwordEncoder.encode("poster-password")));
        UserAccount finder = accounts.save(new UserAccount("Finder", "finder@example.edu", passwordEncoder.encode("finder-password")));
        UserAccount outsider = accounts.save(new UserAccount("Outsider", "outsider@example.edu", passwordEncoder.encode("outsider-password")));

        var createResult = mvc.perform(post("/reports").with(user(poster.getEmail())).with(csrf())
                        .param("type", "LOST")
                        .param("itemName", "Blue water bottle")
                        .param("category", ItemCategory.WATER_BOTTLES.name())
                        .param("description", "Blue metal bottle with a small sticker")
                        .param("lastSeenOrFoundAt", LocalDate.now().toString())
                        .param("location", "Main library"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String reportUrl = createResult.getResponse().getRedirectedUrl();
        Long reportId = Long.valueOf(reportUrl.substring(reportUrl.lastIndexOf('/') + 1));
        Report report = reports.findById(reportId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(poster.getId(), report.getOwner().getId());

        mvc.perform(get("/reports/" + reportId).with(user(finder.getEmail())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Contact the poster privately")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("poster@example.edu"))));

        mvc.perform(post("/reports/" + reportId + "/contact").with(user(finder.getEmail())).with(csrf())
                        .param("body", "I found a bottle near the library. What sticker is on it?"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/inbox/*"));
        ContactThread thread = threads.findAll().get(0);

        mvc.perform(get("/inbox/" + thread.getId()).with(user(finder.getEmail())))
                .andExpect(status().isOk())
                .andExpect(view().name("contact/thread"))
                .andExpect(content().string(containsString("I found a bottle near the library.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("poster@example.edu"))));

        mvc.perform(get("/account").with(user(poster.getEmail())))
                .andExpect(status().isOk()).andExpect(view().name("account/dashboard"));
        mvc.perform(get("/saved").with(user(poster.getEmail())))
                .andExpect(status().isOk()).andExpect(view().name("account/saved"));

        mvc.perform(get("/inbox/" + thread.getId()).with(user(outsider.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));

        var foundResult = mvc.perform(post("/reports").with(user(finder.getEmail())).with(csrf())
                        .param("type", "FOUND")
                        .param("itemName", "Blue water bottle")
                        .param("category", ItemCategory.WATER_BOTTLES.name())
                        .param("description", "Blue metal bottle with a small sticker")
                        .param("lastSeenOrFoundAt", LocalDate.now().toString())
                        .param("location", "Main library"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Long foundId = Long.valueOf(foundResult.getResponse().getRedirectedUrl().substring(foundResult.getResponse().getRedirectedUrl().lastIndexOf('/') + 1));
        mvc.perform(get("/notifications").with(user(poster.getEmail())))
                .andExpect(status().isOk())
                .andExpect(view().name("account/notifications"))
                .andExpect(content().string(containsString("Possible match for")));
        MatchAlert alert = alerts.findAll().get(0);
        mvc.perform(get("/notifications/" + alert.getId()).with(user(poster.getEmail())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/" + foundId));
        org.junit.jupiter.api.Assertions.assertNotNull(alerts.findById(alert.getId()).orElseThrow().getReadAt());
    }

    @Test
    void loginAndRegisterPagesRenderAndReportCreationRequiresLogin() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("auth/login"));
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("auth/register"));
        mvc.perform(get("/reports/new")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/reports")).andExpect(status().isOk()).andExpect(view().name("reports/list"));
    }
}
