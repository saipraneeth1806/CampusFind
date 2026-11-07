package com.example.campusfind.repository;

import com.example.campusfind.model.SavedReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedReportRepository extends JpaRepository<SavedReport, Long> {
    List<SavedReport> findByAccount_IdOrderByCreatedAtDesc(Long accountId);
    Optional<SavedReport> findByAccount_IdAndReport_Id(Long accountId, Long reportId);
    boolean existsByAccount_IdAndReport_Id(Long accountId, Long reportId);
    void deleteByAccount_IdAndReport_Id(Long accountId, Long reportId);
}
