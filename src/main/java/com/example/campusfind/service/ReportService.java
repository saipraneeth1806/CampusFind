package com.example.campusfind.service;

import com.example.campusfind.dto.ReportForm;
import com.example.campusfind.exception.ReportNotFoundException;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportStatus;
import com.example.campusfind.model.ReportType;
import com.example.campusfind.model.ItemCategory;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.ReportRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final ReportRepository repository;
    private final Clock clock;

    public ReportService(ReportRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Report create(ReportForm form, UserAccount owner) {
        return create(form, null, owner);
    }

    @Transactional
    public Report create(ReportForm form, String imageFileName, UserAccount owner) {
        LocalDate today = LocalDate.now(clock);
        if (form.getLastSeenOrFoundAt() == null || form.getLastSeenOrFoundAt().isAfter(today)
                || form.getLastSeenOrFoundAt().isBefore(today.minusDays(365))) {
            throw new IllegalArgumentException("Date must be today or within the last 365 days.");
        }
        Report report = new Report(form.getType(), clean(form.getItemName()), form.getCategory(),
                clean(form.getDescription()), form.getLastSeenOrFoundAt(), clean(form.getLocation()),
                clean(imageFileName), owner);
        if (form.getCustodyStatus() != null) report.setCustodyStatus(form.getCustodyStatus());
        report.setSecretQuestion(clean(form.getSecretQuestion()));
        report.setReward(clean(form.getReward()));
        report.setStudentId(clean(form.getStudentId()));
        report.setCreatedAt(LocalDateTime.now(clock));
        return repository.save(report);
    }

    @Transactional(readOnly = true)
    public Report get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ReportNotFoundException(id));
    }

    @Transactional
    public Report resolve(Long id, UserAccount actor) {
        Report report = get(id);
        if (report.getOwner() == null || !report.getOwner().getId().equals(actor.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Only the report owner can resolve it.");
        }
        report.setStatus(ReportStatus.RESOLVED);
        return repository.save(report);
    }

    @Transactional(readOnly = true)
    public java.util.List<Report> findOwnedBy(UserAccount account) {
        return repository.findByOwner_IdOrderByCreatedAtDesc(account.getId());
    }

    @Transactional(readOnly = true)
    public Page<Report> search(String rawQuery, String type, String category, String status, int page) {
        Sort sort = Sort.by(Sort.Order.desc("lastSeenOrFoundAt"), Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
        return repository.findAll(buildSpecification(rawQuery, type, category, status), PageRequest.of(Math.max(page, 0), 12, sort));
    }

    @Transactional(readOnly = true)
    public java.util.List<Report> searchAll(String rawQuery, String type, String category, String status) {
        Sort sort = Sort.by(Sort.Order.desc("lastSeenOrFoundAt"), Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
        return repository.findAll(buildSpecification(rawQuery, type, category, status), sort);
    }

    public Specification<Report> buildSpecification(String rawQuery, String type, String category, String status) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() > 100) query = query.substring(0, 100);
        String searchText = query;

        return (root, criteria, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (type != null && !type.isBlank()) predicates.add(builder.equal(root.get("type"), ReportType.valueOf(type)));
            if (category != null && !category.isBlank()) predicates.add(builder.equal(root.get("category"), ItemCategory.valueOf(category)));
            String selectedStatus = status == null || status.isBlank() ? ReportStatus.OPEN.name() : status;
            if (!"ALL".equals(selectedStatus)) predicates.add(builder.equal(root.get("status"), ReportStatus.valueOf(selectedStatus)));
            if (!searchText.isBlank()) {
                String escaped = escapeLike(searchText.toLowerCase(java.util.Locale.ROOT));
                var pattern = "%" + escaped + "%";
                var name = builder.lower(root.get("itemName"));
                var description = builder.lower(root.get("description"));
                var location = builder.lower(root.get("location"));
                predicates.add(builder.or(builder.like(name, pattern, '\\'), builder.like(description, pattern, '\\'), builder.like(location, pattern, '\\')));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String clean(String value) { return value == null ? null : value.trim(); }
}
