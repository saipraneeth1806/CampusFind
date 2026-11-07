package com.example.campusfind.service;

import com.example.campusfind.dto.MatchCandidate;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportStatus;
import com.example.campusfind.repository.ReportRepository;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchingService {
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "the", "is", "of", "and", "or", "to", "for", "my", "i", "it", "in", "on", "at", "with");
    private static final Pattern NON_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private final ReportRepository repository;

    public MatchingService(ReportRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<MatchCandidate> findMatches(Report source) {
        if (source.getStatus() != ReportStatus.OPEN) return List.of();
        List<MatchCandidate> matches = new ArrayList<>();
        for (Report candidate : repository.findTop300ByStatusOrderByCreatedAtDesc(ReportStatus.OPEN)) {
            if (candidate.getStatus() != ReportStatus.OPEN
                    || java.util.Objects.equals(candidate.getId(), source.getId())
                    || candidate.getType() == source.getType()) continue;
            if (source.getOwner() != null && candidate.getOwner() != null
                    && source.getOwner().getId().equals(candidate.getOwner().getId())) continue;
            long days = Math.abs(ChronoUnit.DAYS.between(source.getLastSeenOrFoundAt(), candidate.getLastSeenOrFoundAt()));
            if (days > 90) continue;
            matches.add(score(source, candidate, days));
        }
        matches.removeIf(match -> match.score() < 30);
        matches.sort(Comparator.comparingInt(MatchCandidate::score).reversed()
                .thenComparingLong(MatchCandidate::daysApart)
                .thenComparing((MatchCandidate match) -> match.report().getCreatedAt(), Comparator.reverseOrder())
                .thenComparing(match -> match.report().getId()));
        return matches.stream().limit(5).toList();
    }

    public MatchCandidate score(Report source, Report candidate, long days) {
        if (source.hasStudentId() && candidate.hasStudentId()) {
            String id1 = source.getStudentId().trim().toLowerCase(Locale.ROOT);
            String id2 = candidate.getStudentId().trim().toLowerCase(Locale.ROOT);
            if (id1.equals(id2)) {
                return new MatchCandidate(candidate, 100, days, List.of("🎯 Exact Student ID / USN Match (100/100)", "Verified university credential match"));
            }
        }

        int total = 0;
        List<String> reasons = new ArrayList<>();
        if (source.getCategory() == candidate.getCategory()) {
            total += 30;
            reasons.add("Same category (+30)");
        }

        Set<String> first = tokens(source.getItemName() + " " + source.getDescription());
        Set<String> second = tokens(candidate.getItemName() + " " + candidate.getDescription());
        Set<String> union = new HashSet<>(first);
        union.addAll(second);
        Set<String> intersection = new HashSet<>(first);
        intersection.retainAll(second);
        int textPoints = union.isEmpty() ? 0 : (int) Math.round(40.0 * intersection.size() / union.size());
        total += textPoints;
        if (textPoints > 0) reasons.add("Similar item words (" + textPoints + "/40)");

        String firstLocation = normalizeLocation(source.getLocation());
        String secondLocation = normalizeLocation(candidate.getLocation());
        int locationPoints = 0;
        if (!firstLocation.isBlank() && firstLocation.equals(secondLocation)) locationPoints = 20;
        else if (Math.min(firstLocation.length(), secondLocation.length()) >= 4
                && (firstLocation.contains(secondLocation) || secondLocation.contains(firstLocation))) locationPoints = 10;
        total += locationPoints;
        if (locationPoints == 20) reasons.add("Same campus location (+20)");
        else if (locationPoints == 10) reasons.add("Similar campus location (+10)");

        int datePoints = days <= 1 ? 10 : days <= 7 ? 7 : days <= 30 ? 4 : 1;
        total += datePoints;
        reasons.add("Dates are " + days + (days == 1 ? " day" : " days") + " apart (+" + datePoints + ")");
        return new MatchCandidate(candidate, Math.min(total, 100), days, List.copyOf(reasons));
    }

    Set<String> tokens(String text) {
        String normalized = NON_LETTER_OR_DIGIT.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ");
        Set<String> result = new HashSet<>();
        for (String token : normalized.trim().split("\\s+")) {
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) result.add(token);
        }
        return result;
    }

    private String normalizeLocation(String location) {
        return NON_LETTER_OR_DIGIT.matcher(location.toLowerCase(Locale.ROOT)).replaceAll(" ").trim().replaceAll("\\s+", " ");
    }
}
