package com.example.campusfind.controller;

import com.example.campusfind.model.ItemCategory;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportStatus;
import com.example.campusfind.model.ReportType;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.ReportRepository;
import com.example.campusfind.service.AccountService;
import com.example.campusfind.service.ContactService;
import com.example.campusfind.service.MatchAlertService;
import com.example.campusfind.service.ReportService;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API Controller providing real-time notification counts,
 * system health diagnostics for cloud deployments (Render, Railway, Fly.io),
 * and decoupled API endpoints for static/Vercel frontends.
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    private final ReportRepository reportRepository;
    private final ReportService reportService;
    private final AccountService accountService;
    private final ContactService contactService;
    private final MatchAlertService alertService;

    public ApiController(ReportRepository reportRepository, ReportService reportService,
                         AccountService accountService, ContactService contactService,
                         MatchAlertService alertService) {
        this.reportRepository = reportRepository;
        this.reportService = reportService;
        this.accountService = accountService;
        this.contactService = contactService;
        this.alertService = alertService;
    }

    /**
     * Standard cloud health check endpoint.
     * Used by Render, Railway, Kubernetes, AWS, and uptime monitors.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "UP");
        body.put("application", "CampusFind");
        body.put("campus", "DSATM Bengaluru");
        body.put("timestamp", Instant.now().toString());
        body.put("version", "1.0.0");
        return ResponseEntity.ok(body);
    }

    /**
     * Real-time unread messages & match alerts counter.
     * Polled by client-side live-updates.js to display notification badges and toasts.
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Object>> unreadCount(@AuthenticationPrincipal UserDetails principal) {
        Map<String, Object> counts = new HashMap<>();
        if (principal == null) {
            counts.put("authenticated", false);
            counts.put("unreadMessages", 0);
            counts.put("unreadAlerts", 0);
            counts.put("totalUnread", 0);
            return ResponseEntity.ok(counts);
        }

        UserAccount account = accountService.findByEmail(principal.getUsername());
        long unreadMsgs = contactService.totalUnreadMessages(account);
        long unreadAlerts = alertService.unreadCount(account);

        counts.put("authenticated", true);
        counts.put("unreadMessages", unreadMsgs);
        counts.put("unreadAlerts", unreadAlerts);
        counts.put("totalUnread", unreadMsgs + unreadAlerts);
        return ResponseEntity.ok(counts);
    }

    /**
     * Live aggregate statistics across campus reports.
     */
    @GetMapping("/v1/stats")
    public ResponseEntity<Map<String, Object>> campusStats() {
        List<Report> all = reportRepository.findAll();
        long openLost = all.stream().filter(r -> r.getStatus() == ReportStatus.OPEN && r.getType() == ReportType.LOST).count();
        long openFound = all.stream().filter(r -> r.getStatus() == ReportStatus.OPEN && r.getType() == ReportType.FOUND).count();
        long resolved = all.stream().filter(r -> r.getStatus() == ReportStatus.RESOLVED).count();
        int total = all.size();
        double recoveryRate = total > 0 ? Math.round((resolved * 100.0 / total) * 10.0) / 10.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeLostReports", openLost);
        stats.put("activeFoundReports", openFound);
        stats.put("resolvedReports", resolved);
        stats.put("totalReports", total);
        stats.put("recoveryRatePercentage", recoveryRate);
        return ResponseEntity.ok(stats);
    }

    /**
     * Decoupled REST endpoint to search and paginate reports.
     */
    @GetMapping("/v1/reports")
    public ResponseEntity<Map<String, Object>> listReports(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page) {

        Page<Report> result = reportService.search(q, type, category, status, page);

        List<Map<String, Object>> items = result.getContent().stream().map(r -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", r.getId());
            dto.put("type", r.getType().name());
            dto.put("itemName", r.getItemName());
            dto.put("category", r.getCategory().name());
            dto.put("categoryLabel", r.getCategory().getLabel());
            dto.put("description", r.getDescription());
            dto.put("location", r.getLocation());
            dto.put("date", r.getLastSeenOrFoundAt().toString());
            dto.put("status", r.getStatus().name());
            dto.put("custodyStatus", r.getCustodyStatus().name());
            dto.put("custodyLabel", r.getCustodyLabel());
            dto.put("hasReward", r.hasReward());
            dto.put("reward", r.getReward());
            dto.put("hasSecretQuestion", r.hasSecretQuestion());
            dto.put("maskedStudentId", r.getMaskedStudentId());
            dto.put("retentionDaysRemaining", r.getDaysRemainingInRetention());
            dto.put("hasImage", r.hasImage());
            dto.put("imageUrl", r.getImageUrl());
            return dto;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("reports", items);
        response.put("currentPage", result.getNumber());
        response.put("totalPages", result.getTotalPages());
        response.put("totalElements", result.getTotalElements());
        return ResponseEntity.ok(response);
    }

    /**
     * Decoupled REST endpoint for a single report details.
     */
    @GetMapping("/v1/reports/{id}")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable Long id) {
        Report r = reportService.get(id);
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", r.getId());
        dto.put("type", r.getType().name());
        dto.put("itemName", r.getItemName());
        dto.put("category", r.getCategory().name());
        dto.put("categoryLabel", r.getCategory().getLabel());
        dto.put("description", r.getDescription());
        dto.put("location", r.getLocation());
        dto.put("date", r.getLastSeenOrFoundAt().toString());
        dto.put("status", r.getStatus().name());
        dto.put("custodyStatus", r.getCustodyStatus().name());
        dto.put("custodyLabel", r.getCustodyLabel());
        dto.put("custodyDescription", r.getCustodyDescription());
        dto.put("hasReward", r.hasReward());
        dto.put("reward", r.getReward());
        dto.put("hasSecretQuestion", r.hasSecretQuestion());
        dto.put("secretQuestion", r.getSecretQuestion());
        dto.put("hasStudentId", r.hasStudentId());
        dto.put("maskedStudentId", r.getMaskedStudentId());
        dto.put("retentionDaysRemaining", r.getDaysRemainingInRetention());
        dto.put("hasImage", r.hasImage());
        dto.put("imageUrl", r.getImageUrl());
        dto.put("hasOwner", r.getOwner() != null);
        return ResponseEntity.ok(dto);
    }

    /**
     * Categories metadata endpoint.
     */
    @GetMapping("/v1/categories")
    public ResponseEntity<List<Map<String, String>>> categories() {
        List<Map<String, String>> list = Arrays.stream(ItemCategory.values())
                .map(c -> Map.of("code", c.name(), "label", c.getLabel()))
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Official RFC 4180 CSV export of campus reports for security desk / administrative ledgers.
     */
    @GetMapping(value = "/v1/reports/export.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> exportReportsCsv(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {

        List<Report> reports = reportService.searchAll(q, type, category, status);
        StringBuilder sb = new StringBuilder();
        sb.append("Report ID,Type,Item Name,Category,Location,Date,Status,Custody,Reward,Student ID,Description\r\n");
        for (Report r : reports) {
            sb.append(r.getId()).append(",");
            sb.append(escapeCsv(r.getType().name())).append(",");
            sb.append(escapeCsv(r.getItemName())).append(",");
            sb.append(escapeCsv(r.getCategory().getLabel())).append(",");
            sb.append(escapeCsv(r.getLocation())).append(",");
            sb.append(escapeCsv(r.getLastSeenOrFoundAt().toString())).append(",");
            sb.append(escapeCsv(r.getStatus().name())).append(",");
            sb.append(escapeCsv(r.getCustodyLabel())).append(",");
            sb.append(escapeCsv(r.getReward() != null ? r.getReward() : "")).append(",");
            sb.append(escapeCsv(r.getMaskedStudentId() != null ? r.getMaskedStudentId() : "")).append(",");
            sb.append(escapeCsv(r.getDescription() != null ? r.getDescription() : "")).append("\r\n");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"campusfind-reports.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(sb.toString());
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}

