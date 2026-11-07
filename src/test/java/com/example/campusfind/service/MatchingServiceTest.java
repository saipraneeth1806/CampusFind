package com.example.campusfind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.campusfind.dto.MatchCandidate;
import com.example.campusfind.model.ItemCategory;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportStatus;
import com.example.campusfind.model.ReportType;
import com.example.campusfind.repository.ReportRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MatchingServiceTest {
    private final ReportRepository repository = org.mockito.Mockito.mock(ReportRepository.class);
    private final MatchingService service = new MatchingService(repository);

    @Test
    void perfectSignalsProduceOneHundred() {
        Report lost = report(1L, ReportType.LOST, ItemCategory.ELECTRONICS, "Black wireless earbuds", "Black wireless earbuds in a case", "Library second floor", LocalDate.of(2026, 3, 10));
        Report found = report(2L, ReportType.FOUND, ItemCategory.ELECTRONICS, "Black wireless earbuds", "Black wireless earbuds in a case", "Library second floor", LocalDate.of(2026, 3, 10));

        MatchCandidate result = service.score(lost, found, 0);

        assertEquals(100, result.score());
        assertEquals(4, result.reasons().size());
    }

    @Test
    void matchingStudentIdProducesExactOneHundredMatch() {
        Report lost = report(1L, ReportType.LOST, ItemCategory.ID_CARDS, "Lost Student ID", "Plastic ID card", "Cafeteria", LocalDate.of(2026, 3, 10));
        lost.setStudentId("1DS22CS104");

        Report found = report(2L, ReportType.FOUND, ItemCategory.ID_CARDS, "Found Card", "Blue lanyard ID card", "Hostel Gate", LocalDate.of(2026, 3, 15));
        found.setStudentId("1DS22CS104");

        MatchCandidate result = service.score(lost, found, 5);

        assertEquals(100, result.score());
        assertTrue(result.reasons().stream().anyMatch(r -> r.contains("Student ID / USN Match")));
    }

    @Test
    void scoreUsesJaccardAndLocationDateBuckets() {
        Report source = report(null, ReportType.LOST, ItemCategory.BAGS, "Blue backpack", "Water bottle inside", "North library", LocalDate.of(2026, 3, 10));
        Report candidate = report(2L, ReportType.FOUND, ItemCategory.OTHER, "Blue bag", "Bottle found", "Gym", LocalDate.of(2026, 3, 18));

        MatchCandidate result = service.score(source, candidate, 8);

        assertEquals(15, result.score()); // Two shared tokens out of seven unique tokens: round(80 / 7) + 0 + 0 + 4.
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("Dates are 8 days")));
        assertFalse(result.reasons().stream().anyMatch(reason -> reason.contains("category")));
    }

    @Test
    void locationNormalizationAndPartialMatchWork() {
        Report source = report(null, ReportType.LOST, ItemCategory.OTHER, "Green notebook", "Small green notebook", "Main Library, floor 2", LocalDate.of(2026, 3, 10));
        Report candidate = report(2L, ReportType.FOUND, ItemCategory.OTHER, "Green notebook", "Small green notebook", "main library", LocalDate.of(2026, 3, 10));

        MatchCandidate result = service.score(source, candidate, 1);

        assertTrue(result.reasons().contains("Similar campus location (+10)"));
        assertEquals(90, result.score());
    }

    @Test
    void excludesSameTypeSelfAndOldCandidatesThenLimitsToFive() {
        Report source = report(1L, ReportType.LOST, ItemCategory.ELECTRONICS, "Black earbuds", "Black wireless earbuds case", "Library", LocalDate.of(2026, 3, 10));
        Report sameType = report(2L, ReportType.LOST, ItemCategory.ELECTRONICS, "Black earbuds", "Black wireless earbuds case", "Library", LocalDate.of(2026, 3, 10));
        Report tooOld = report(3L, ReportType.FOUND, ItemCategory.ELECTRONICS, "Black earbuds", "Black wireless earbuds case", "Library", LocalDate.of(2025, 11, 30));
        List<Report> candidates = new java.util.ArrayList<>();
        candidates.add(source);
        candidates.add(sameType);
        candidates.add(tooOld);
        for (long id = 4; id <= 10; id++) candidates.add(report(id, ReportType.FOUND, ItemCategory.ELECTRONICS,
                "Black earbuds", "Black wireless earbuds case", "Library", LocalDate.of(2026, 3, 10)));
        when(repository.findTop300ByStatusOrderByCreatedAtDesc(ReportStatus.OPEN)).thenReturn(candidates);

        List<MatchCandidate> matches = service.findMatches(source);

        assertEquals(5, matches.size());
        assertTrue(matches.stream().allMatch(match -> match.report().getType() == ReportType.FOUND));
    }

    @Test
    void datePointBandsMatchSpecification() {
        Report source = report(null, ReportType.LOST, ItemCategory.OTHER, "Marker", "Blue ink", "Unknown room", LocalDate.of(2026, 3, 10));
        Report candidate = report(2L, ReportType.FOUND, ItemCategory.BAGS, "Wallet", "Brown leather", "Elsewhere", LocalDate.of(2026, 3, 10));

        assertEquals(10, service.score(source, candidate, 0).score());
        assertEquals(10, service.score(source, candidate, 1).score());
        assertEquals(7, service.score(source, candidate, 2).score());
        assertEquals(7, service.score(source, candidate, 7).score());
        assertEquals(4, service.score(source, candidate, 8).score());
        assertEquals(4, service.score(source, candidate, 30).score());
        assertEquals(1, service.score(source, candidate, 31).score());
        assertEquals(1, service.score(source, candidate, 90).score());
    }

    @Test
    void emptyNormalizedTextHasZeroTextPoints() {
        Report source = report(null, ReportType.LOST, ItemCategory.OTHER, "A", "I the", "North hall", LocalDate.of(2026, 3, 10));
        Report candidate = report(2L, ReportType.FOUND, ItemCategory.BAGS, "An", "It of", "South gym", LocalDate.of(2026, 3, 10));

        MatchCandidate result = service.score(source, candidate, 0);

        assertEquals(10, result.score()); // Date points only; all text tokens are removed.
        assertFalse(result.reasons().stream().anyMatch(reason -> reason.contains("words")));
    }

    @Test
    void exactAndUnrelatedLocationsHaveExpectedPoints() {
        Report source = report(null, ReportType.LOST, ItemCategory.OTHER, "Marker", "Blue ink", "Science Building, room 12", LocalDate.of(2026, 3, 10));
        Report exact = report(2L, ReportType.FOUND, ItemCategory.BAGS, "Wallet", "Brown leather", "science-building room 12", LocalDate.of(2026, 3, 10));
        Report unrelated = report(3L, ReportType.FOUND, ItemCategory.BAGS, "Wallet", "Brown leather", "Gym", LocalDate.of(2026, 3, 10));

        assertTrue(service.score(source, exact, 0).reasons().contains("Same campus location (+20)"));
        assertEquals(30, service.score(source, exact, 0).score());
        assertEquals(10, service.score(source, unrelated, 0).score());
    }

    @Test
    void weakCandidatesAreRemovedAndResolvedCandidatesAreIgnored() {
        Report source = report(1L, ReportType.LOST, ItemCategory.ELECTRONICS, "Wireless earbuds", "Black charging case", "Library", LocalDate.of(2026, 3, 10));
        Report weak = report(2L, ReportType.FOUND, ItemCategory.BAGS, "Notebook", "Blue cover", "Gym", LocalDate.of(2026, 3, 10));
        Report resolved = report(3L, ReportType.FOUND, ItemCategory.ELECTRONICS, "Wireless earbuds", "Black charging case", "Library", LocalDate.of(2026, 3, 10));
        resolved.setStatus(ReportStatus.RESOLVED);
        when(repository.findTop300ByStatusOrderByCreatedAtDesc(ReportStatus.OPEN)).thenReturn(List.of(source, weak, resolved));

        assertTrue(service.findMatches(source).isEmpty());
    }

    @Test
    void resultsSortByScoreThenDateDistanceThenNewestCreation() {
        Report source = report(1L, ReportType.LOST, ItemCategory.ELECTRONICS, "Wireless earbuds", "Black charging case", "Library", LocalDate.of(2026, 3, 10));
        Report farther = report(2L, ReportType.FOUND, ItemCategory.ELECTRONICS, "Wireless earbuds", "Black charging case", "Library", LocalDate.of(2026, 3, 12));
        Report closerOlder = report(3L, ReportType.FOUND, ItemCategory.ELECTRONICS, "Wireless earbuds", "Black charging case", "Library", LocalDate.of(2026, 3, 10));
        Report closerNewer = report(4L, ReportType.FOUND, ItemCategory.ELECTRONICS, "Wireless earbuds", "Black charging case", "Library", LocalDate.of(2026, 3, 10));
        ReflectionTestUtils.setField(closerOlder, "createdAt", LocalDateTime.of(2026, 3, 1, 12, 0));
        ReflectionTestUtils.setField(closerNewer, "createdAt", LocalDateTime.of(2026, 3, 2, 12, 0));
        when(repository.findTop300ByStatusOrderByCreatedAtDesc(ReportStatus.OPEN)).thenReturn(List.of(source, farther, closerOlder, closerNewer));

        List<MatchCandidate> matches = service.findMatches(source);

        assertEquals(List.of(4L, 3L, 2L), matches.stream().map(match -> match.report().getId()).toList());
    }

    private Report report(Long id, ReportType type, ItemCategory category, String name, String description, String location, LocalDate date) {
        Report report = new Report(type, name, category, description, date, location, null);
        if (id != null) ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 3, 10, 12, 0).plusMinutes(id == null ? 0 : id));
        return report;
    }
}
