package com.example.campusfind.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_alerts", uniqueConstraints = @UniqueConstraint(name = "uk_match_alert_pair",
        columnNames = {"recipient_id", "new_report_id", "matched_report_id"}))
public class MatchAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "new_report_id", nullable = false)
    private Report newReport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matched_report_id", nullable = false)
    private Report matchedReport;

    @Column(nullable = false)
    private int score;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected MatchAlert() { }

    public MatchAlert(UserAccount recipient, Report newReport, Report matchedReport, int score) {
        this.recipient = recipient;
        this.newReport = newReport;
        this.matchedReport = matchedReport;
        this.score = score;
    }

    @PrePersist
    void setCreatedAtIfMissing() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UserAccount getRecipient() { return recipient; }
    public Report getNewReport() { return newReport; }
    public Report getMatchedReport() { return matchedReport; }
    public int getScore() { return score; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
