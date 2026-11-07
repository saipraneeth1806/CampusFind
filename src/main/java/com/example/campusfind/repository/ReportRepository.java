package com.example.campusfind.repository;

import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {
    List<Report> findTop300ByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<Report> findTop6ByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<Report> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);
    Page<Report> findAll(Specification<Report> specification, Pageable pageable);
}
