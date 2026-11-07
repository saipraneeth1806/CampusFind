package com.example.campusfind.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthCheckShouldReturnUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("CampusFind"))
                .andExpect(jsonPath("$.campus").value("DSATM Bengaluru"));
    }

    @Test
    void campusStatsShouldReturnAggregateMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").isNumber())
                .andExpect(jsonPath("$.activeLostReports").isNumber())
                .andExpect(jsonPath("$.activeFoundReports").isNumber());
    }

    @Test
    void categoriesShouldReturnAvailableItemCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists())
                .andExpect(jsonPath("$[0].label").exists());
    }

    @Test
    void anonymousUnreadCountShouldReturnZeroMetrics() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.totalUnread").value(0));
    }

    @Test
    void exportCsvShouldReturnCsvLedger() throws Exception {
        mockMvc.perform(get("/api/v1/reports/export.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().string(containsString("Report ID,Type,Item Name,Category,Location,Date,Status,Custody,Reward,Student ID,Description")));
    }
}

