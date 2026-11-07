package com.example.campusfind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.campusfind.dto.ReportForm;
import com.example.campusfind.exception.ReportNotFoundException;
import com.example.campusfind.model.ItemCategory;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportStatus;
import com.example.campusfind.model.ReportType;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.ReportRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReportServiceTest {
    private final ReportRepository repository = org.mockito.Mockito.mock(ReportRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC);
    private final ReportService service = new ReportService(repository, clock);
    private ReportForm form;

    @BeforeEach
    void setUp() {
        form = new ReportForm();
        form.setType(ReportType.LOST);
        form.setItemName("  Black earbuds  ");
        form.setCategory(ItemCategory.ELECTRONICS);
        form.setDescription("  Black wireless earbuds in a case  ");
        form.setLastSeenOrFoundAt(LocalDate.of(2026, 3, 10));
        form.setLocation("  Library  ");
    }

    @Test
    void createTrimsFieldsAndSetsOpenStatusAndTime() {
        when(repository.save(org.mockito.ArgumentMatchers.any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report saved = service.create(form, null);

        assertEquals("Black earbuds", saved.getItemName());
        assertEquals("Black wireless earbuds in a case", saved.getDescription());
        assertEquals("Library", saved.getLocation());
        assertEquals(null, saved.getOwner());
        assertEquals(ReportStatus.OPEN, saved.getStatus());
        assertEquals(LocalDateTime.of(2026, 3, 10, 12, 0), saved.getCreatedAt());
        verify(repository).save(saved);
    }

    @Test
    void createSetsCustodyRewardAndStudentId() {
        when(repository.save(org.mockito.ArgumentMatchers.any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        form.setLastSeenOrFoundAt(LocalDate.now(clock));
        form.setCustodyStatus(com.example.campusfind.model.CustodyStatus.SECURITY_DESK);
        form.setReward("₹500 Cash");
        form.setSecretQuestion("What color is the sticker?");
        form.setStudentId("1DS21CS045");

        Report saved = service.create(form, null);

        assertEquals(com.example.campusfind.model.CustodyStatus.SECURITY_DESK, saved.getCustodyStatus());
        assertEquals("Campus Security Office (Gate 1)", saved.getCustodyLabel());
        assertEquals("₹500 Cash", saved.getReward());
        assertTrue(saved.hasReward());
        assertEquals("What color is the sticker?", saved.getSecretQuestion());
        assertTrue(saved.hasSecretQuestion());
        assertEquals("1DS21***", saved.getMaskedStudentId());
        assertTrue(saved.hasStudentId());
        assertEquals(60, saved.getDaysRemainingInRetention(LocalDate.now(clock)));
    }

    @Test
    void createWithImageFileNameSetsImageAndProvidesUrl() {
        when(repository.save(org.mockito.ArgumentMatchers.any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report saved = service.create(form, "earbuds-123.jpg", null);

        assertEquals("earbuds-123.jpg", saved.getImageFileName());
        org.junit.jupiter.api.Assertions.assertTrue(saved.hasImage());
        assertEquals("/uploads/earbuds-123.jpg", saved.getImageUrl());
    }

    @Test
    void rejectsFutureAndOlderThanOneYearDates() {
        form.setLastSeenOrFoundAt(LocalDate.of(2026, 3, 11));
        assertThrows(IllegalArgumentException.class, () -> service.create(form, null));
        form.setLastSeenOrFoundAt(LocalDate.of(2025, 3, 9));
        assertThrows(IllegalArgumentException.class, () -> service.create(form, null));
    }

    @Test
    void resolvingAlreadyResolvedReportIsSafe() {
        UserAccount owner = new UserAccount("Student", "student@example.edu", "hash");
        ReflectionTestUtils.setField(owner, "id", 7L);
        Report report = new Report(ReportType.LOST, "Black earbuds", ItemCategory.ELECTRONICS,
                "Black wireless earbuds", LocalDate.of(2026, 3, 10), "Library", owner);
        report.setStatus(ReportStatus.RESOLVED);
        when(repository.findById(17L)).thenReturn(Optional.of(report));
        when(repository.save(report)).thenReturn(report);

        assertEquals(ReportStatus.RESOLVED, service.resolve(17L, owner).getStatus());
        verify(repository).save(report);
    }

    @Test
    void missingReportUsesFriendlyNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        UserAccount owner = new UserAccount("Student", "student@example.edu", "hash");
        assertThrows(ReportNotFoundException.class, () -> service.resolve(99L, owner));
    }
}
