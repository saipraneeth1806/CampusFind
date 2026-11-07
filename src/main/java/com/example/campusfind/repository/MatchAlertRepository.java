package com.example.campusfind.repository;

import com.example.campusfind.model.MatchAlert;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchAlertRepository extends JpaRepository<MatchAlert, Long> {
    List<MatchAlert> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId);
    Optional<MatchAlert> findByIdAndRecipient_Id(Long id, Long recipientId);
    boolean existsByRecipient_IdAndNewReport_IdAndMatchedReport_Id(Long recipientId, Long newReportId, Long matchedReportId);
    long countByRecipient_IdAndReadAtIsNull(Long recipientId);
}
