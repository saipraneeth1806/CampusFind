package com.example.campusfind.controller;

import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportStatus;
import com.example.campusfind.model.ReportType;
import com.example.campusfind.repository.ReportRepository;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final ReportRepository repository;

    public HomeController(ReportRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Report> all = repository.findAll();
        long openLost = all.stream().filter(r -> r.getStatus() == ReportStatus.OPEN && r.getType() == ReportType.LOST).count();
        long openFound = all.stream().filter(r -> r.getStatus() == ReportStatus.OPEN && r.getType() == ReportType.FOUND).count();
        long resolved = all.stream().filter(r -> r.getStatus() == ReportStatus.RESOLVED).count();
        int total = all.size();
        double recoveryRate = total > 0 ? Math.round((resolved * 100.0 / total) * 10.0) / 10.0 : 0.0;

        model.addAttribute("recentReports", repository.findTop6ByStatusOrderByCreatedAtDesc(ReportStatus.OPEN));
        model.addAttribute("openLostCount", openLost);
        model.addAttribute("openFoundCount", openFound);
        model.addAttribute("resolvedCount", resolved);
        model.addAttribute("totalReportsCount", total);
        model.addAttribute("recoveryRate", recoveryRate);
        return "index";
    }
}
