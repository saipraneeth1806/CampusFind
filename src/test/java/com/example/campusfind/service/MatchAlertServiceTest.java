package com.example.campusfind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.campusfind.dto.MatchCandidate;
import com.example.campusfind.model.ItemCategory;
import com.example.campusfind.model.MatchAlert;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportType;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.MatchAlertRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class MatchAlertServiceTest {
    private final MatchAlertRepository alerts = org.mockito.Mockito.mock(MatchAlertRepository.class);
    private final MatchingService matching = org.mockito.Mockito.mock(MatchingService.class);
    private final MatchAlertService service = new MatchAlertService(alerts, matching,
            Clock.system(ZoneOffset.UTC));

    @Test
    void onlyStrongMatchesNotifyOtherReportOwners() {
        UserAccount newPoster = account(1L, "new@example.edu");
        UserAccount priorPoster = account(2L, "prior@example.edu");
        Report newReport = report(10L, ReportType.FOUND, newPoster);
        Report strongMatch = report(20L, ReportType.LOST, priorPoster);
        Report weakMatch = report(30L, ReportType.LOST, account(3L, "weak@example.edu"));
        when(matching.findMatches(newReport)).thenReturn(List.of(
                new MatchCandidate(strongMatch, 82, 1, List.of("Same category")),
                new MatchCandidate(weakMatch, 55, 2, List.of("Similar words"))));
        when(alerts.existsByRecipient_IdAndNewReport_IdAndMatchedReport_Id(2L, 10L, 20L)).thenReturn(false);
        when(alerts.existsByRecipient_IdAndNewReport_IdAndMatchedReport_Id(3L, 10L, 30L)).thenReturn(false);

        service.notifyOwnersOfMatches(newReport);

        ArgumentCaptor<MatchAlert> saved = ArgumentCaptor.forClass(MatchAlert.class);
        verify(alerts).save(saved.capture());
        assertEquals(priorPoster.getId(), saved.getValue().getRecipient().getId());
        assertEquals(82, saved.getValue().getScore());
        verify(alerts, never()).existsByRecipient_IdAndNewReport_IdAndMatchedReport_Id(3L, 10L, 30L);
    }

    private UserAccount account(long id, String email) {
        UserAccount account = new UserAccount("Member", email, "hash");
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Report report(long id, ReportType type, UserAccount owner) {
        Report report = new Report(type, "Blue bottle", ItemCategory.WATER_BOTTLES,
                "Blue metal bottle", LocalDate.of(2026, 3, 10), "Library", owner);
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }
}
