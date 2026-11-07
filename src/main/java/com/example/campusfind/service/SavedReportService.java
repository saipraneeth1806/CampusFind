package com.example.campusfind.service;

import com.example.campusfind.exception.ReportNotFoundException;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.SavedReport;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.ReportRepository;
import com.example.campusfind.repository.SavedReportRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedReportService {
    private final SavedReportRepository savedReports;
    private final ReportRepository reports;

    public SavedReportService(SavedReportRepository savedReports, ReportRepository reports) {
        this.savedReports = savedReports;
        this.reports = reports;
    }

    @Transactional
    public boolean toggle(UserAccount account, Long reportId) {
        if (savedReports.existsByAccount_IdAndReport_Id(account.getId(), reportId)) {
            savedReports.deleteByAccount_IdAndReport_Id(account.getId(), reportId);
            return false;
        }
        Report report = reports.findById(reportId).orElseThrow(() -> new ReportNotFoundException(reportId));
        savedReports.save(new SavedReport(account, report));
        return true;
    }

    @Transactional(readOnly = true)
    public List<SavedReport> forAccount(UserAccount account) {
        return savedReports.findByAccount_IdOrderByCreatedAtDesc(account.getId());
    }

    @Transactional(readOnly = true)
    public boolean isSaved(UserAccount account, Long reportId) {
        return savedReports.existsByAccount_IdAndReport_Id(account.getId(), reportId);
    }
}
