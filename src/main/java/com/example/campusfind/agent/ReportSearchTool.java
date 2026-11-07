package com.example.campusfind.agent;

import com.example.campusfind.model.Report;
import com.example.campusfind.service.ReportService;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.example.campusfind.repository.ReportRepository;
import org.springframework.stereotype.Component;

/** The read-only report search and statistics service used by the CampusFind assistant. */
@Component
public class ReportSearchTool {
    private static final Pattern WORDS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "and", "are", "at", "did", "do", "for", "found", "have", "help", "i", "in", "is", "it", "looking", "lost", "me", "my", "of", "on", "please", "search", "the", "there", "to", "what", "where", "with");
    private final ReportService reports;
    private final ReportRepository reportRepository;

    public ReportSearchTool(ReportService reports, ReportRepository reportRepository) {
        this.reports = reports;
        this.reportRepository = reportRepository;
    }

    public List<AgentSearchResult> search(String query, String reportType) {
        String safeQuery = query == null ? "" : query.strip();
        if (safeQuery.length() > 200) safeQuery = safeQuery.substring(0, 200);
        String safeType = normalizeType(reportType);
        List<String> terms = terms(safeQuery);
        if (terms.isEmpty()) return List.of();

        Map<Long, Report> candidates = new LinkedHashMap<>();
        for (String term : terms) {
            for (Report report : reports.search(term, safeType, null, "OPEN", 0).getContent()) {
                candidates.putIfAbsent(report.getId(), report);
            }
        }
        if (candidates.isEmpty() && safeType != null) {
            for (String term : terms) {
                for (Report report : reports.search(term, null, null, "OPEN", 0).getContent()) {
                    candidates.putIfAbsent(report.getId(), report);
                }
            }
        }
        return candidates.values().stream()
                .map(report -> toResult(report, terms))
                .sorted(Comparator.comparingInt(AgentSearchResult::relevance).reversed()
                        .thenComparing(AgentSearchResult::date, Comparator.reverseOrder())
                        .thenComparing(AgentSearchResult::id))
                .limit(8)
                .toList();
    }

    private AgentSearchResult toResult(Report report, List<String> terms) {
        String searchable = normalize(report.getItemName() + " " + report.getLocation() + " " + report.getCategory().getLabel());
        int relevance = (int) terms.stream().filter(searchable::contains).count();
        return new AgentSearchResult(report.getId(), report.getType().name(), report.getItemName(),
                report.getCategory().getLabel(), report.getLocation(), report.getLastSeenOrFoundAt().toString(),
                report.getStatus().name(), relevance, report.getImageUrl());
    }

    private List<String> terms(String query) {
        List<String> result = new ArrayList<>();
        for (String part : normalize(query).split("\\s+")) {
            if (part.length() >= 3 && !STOP_WORDS.contains(part) && !result.contains(part)) result.add(part);
            if (result.size() == 6) break;
        }
        return result;
    }

    private String normalize(String value) {
        return WORDS.matcher(Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFKC)).replaceAll(" ").trim().replaceAll("\\s+", " ");
    }

    private String normalizeType(String value) {
        if (value == null || value.isBlank() || "ANY".equalsIgnoreCase(value)) return null;
        String type = value.strip().toUpperCase(Locale.ROOT);
        return Set.of("LOST", "FOUND").contains(type) ? type : null;
    }

    public AgentSearchResult getReportDetails(Long id) {
        if (id == null || reportRepository == null) return null;
        return reportRepository.findById(id)
                .map(r -> new AgentSearchResult(r.getId(), r.getType().name(), r.getItemName(),
                        r.getCategory().getLabel(), r.getLocation(), r.getLastSeenOrFoundAt().toString(),
                        r.getStatus().name(), 1, r.getImageUrl()))
                .orElse(null);
    }

    public String getCampusStats() {
        if (reportRepository == null) return "{}";
        var all = reportRepository.findAll();
        long openLost = all.stream().filter(r -> r.getStatus() == com.example.campusfind.model.ReportStatus.OPEN && r.getType() == com.example.campusfind.model.ReportType.LOST).count();
        long openFound = all.stream().filter(r -> r.getStatus() == com.example.campusfind.model.ReportStatus.OPEN && r.getType() == com.example.campusfind.model.ReportType.FOUND).count();
        long resolved = all.stream().filter(r -> r.getStatus() == com.example.campusfind.model.ReportStatus.RESOLVED).count();
        return String.format("{\"activeLostReports\":%d,\"activeFoundReports\":%d,\"resolvedReports\":%d,\"totalReports\":%d}",
                openLost, openFound, resolved, all.size());
    }
}
