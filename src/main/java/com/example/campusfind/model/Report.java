package com.example.campusfind.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_status_created", columnList = "status,created_at"),
        @Index(name = "idx_reports_type_status", columnList = "type,status"),
        @Index(name = "idx_reports_category", columnList = "category")
})
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportType type;

    @Column(name = "item_name", nullable = false, length = 80)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemCategory category;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "last_seen_or_found_at", nullable = false)
    private LocalDate lastSeenOrFoundAt;

    @Column(nullable = false, length = 100)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;

    @Column(name = "image_file_name", length = 120)
    private String imageFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status = ReportStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "custody_status", length = 30)
    private CustodyStatus custodyStatus = CustodyStatus.WITH_FINDER;

    @Column(name = "secret_question", length = 200)
    private String secretQuestion;

    @Column(name = "reward", length = 100)
    private String reward;

    @Column(name = "student_id", length = 50)
    private String studentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Report() { }

    public Report(ReportType type, String itemName, ItemCategory category, String description,
                  LocalDate lastSeenOrFoundAt, String location, UserAccount owner) {
        this(type, itemName, category, description, lastSeenOrFoundAt, location, null, owner);
    }

    public Report(ReportType type, String itemName, ItemCategory category, String description,
                  LocalDate lastSeenOrFoundAt, String location, String imageFileName, UserAccount owner) {
        this.type = type;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.lastSeenOrFoundAt = lastSeenOrFoundAt;
        this.location = location;
        this.imageFileName = imageFileName;
        this.owner = owner;
        this.status = ReportStatus.OPEN;
    }

    @PrePersist
    void setCreatedAtIfMissing() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public ReportType getType() { return type; }
    public String getItemName() { return itemName; }
    public ItemCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public LocalDate getLastSeenOrFoundAt() { return lastSeenOrFoundAt; }
    public String getLocation() { return location; }
    public UserAccount getOwner() { return owner; }
    public String getImageFileName() { return imageFileName; }
    public void setImageFileName(String imageFileName) { this.imageFileName = imageFileName; }
    public boolean hasImage() { return imageFileName != null && !imageFileName.isBlank(); }
    public String getImageUrl() { return hasImage() ? "/uploads/" + imageFileName : null; }
    public ReportStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public CustodyStatus getCustodyStatus() { return custodyStatus != null ? custodyStatus : CustodyStatus.WITH_FINDER; }
    public void setCustodyStatus(CustodyStatus custodyStatus) { this.custodyStatus = custodyStatus; }
    public String getCustodyLabel() { return getCustodyStatus().getLabel(); }
    public String getCustodyDescription() { return getCustodyStatus().getDescription(); }

    public String getSecretQuestion() { return secretQuestion; }
    public void setSecretQuestion(String secretQuestion) { this.secretQuestion = secretQuestion; }
    public boolean hasSecretQuestion() { return secretQuestion != null && !secretQuestion.isBlank(); }

    public String getReward() { return reward; }
    public void setReward(String reward) { this.reward = reward; }
    public boolean hasReward() { return reward != null && !reward.isBlank(); }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public boolean hasStudentId() { return studentId != null && !studentId.isBlank(); }

    public String getMaskedStudentId() {
        if (!hasStudentId()) return null;
        String trimmed = studentId.trim();
        if (trimmed.length() <= 4) return "****";
        int visiblePrefix = Math.min(trimmed.length() - 3, 5);
        return trimmed.substring(0, visiblePrefix) + "***";
    }

    public long getDaysRemainingInRetention() {
        return getDaysRemainingInRetention(LocalDate.now());
    }

    public long getDaysRemainingInRetention(LocalDate referenceDate) {
        if (lastSeenOrFoundAt == null) return 60;
        long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(lastSeenOrFoundAt, referenceDate != null ? referenceDate : LocalDate.now());
        return Math.max(0, 60 - daysPassed);
    }

    public boolean isExpiringSoon() {
        long rem = getDaysRemainingInRetention();
        return rem <= 10 && rem > 0;
    }
}
