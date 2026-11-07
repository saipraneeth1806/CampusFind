package com.example.campusfind.service;

import com.example.campusfind.exception.ReportNotFoundException;
import com.example.campusfind.model.MatchAlert;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.MatchAlertRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchAlertService {
    private final MatchAlertRepository alerts;
    private final MatchingService matching;
    private final Clock clock;

    public MatchAlertService(MatchAlertRepository alerts, MatchingService matching, Clock clock) {
        this.alerts = alerts;
        this.matching = matching;
        this.clock = clock;
    }

    @Transactional
    public void notifyOwnersOfMatches(Report newReport) {
        if (newReport.getOwner() == null) return;
        for (var match : matching.findMatches(newReport)) {
            if (match.score() < 60) continue;
            Report priorReport = match.report();
            UserAccount recipient = priorReport.getOwner();
            if (recipient == null || recipient.getId().equals(newReport.getOwner().getId())) continue;
            if (alerts.existsByRecipient_IdAndNewReport_IdAndMatchedReport_Id(recipient.getId(), newReport.getId(), priorReport.getId())) continue;
            alerts.save(new MatchAlert(recipient, newReport, priorReport, match.score()));
        }
    }

    @Transactional(readOnly = true)
    public List<MatchAlert> forAccount(UserAccount account) {
        return alerts.findByRecipient_IdOrderByCreatedAtDesc(account.getId());
    }

    @Transactional(readOnly = true)
    public long unreadCount(UserAccount account) {
        return alerts.countByRecipient_IdAndReadAtIsNull(account.getId());
    }

    @Transactional
    public MatchAlert open(Long alertId, UserAccount account) {
        MatchAlert alert = alerts.findByIdAndRecipient_Id(alertId, account.getId())
                .orElseThrow(() -> new ReportNotFoundException(alertId));
        if (alert.getReadAt() == null) alert.setReadAt(LocalDateTime.now(clock));
        return alert;
    }
}
