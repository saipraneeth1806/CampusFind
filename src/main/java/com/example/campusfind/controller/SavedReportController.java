package com.example.campusfind.controller;

import com.example.campusfind.model.UserAccount;
import com.example.campusfind.service.AccountService;
import com.example.campusfind.service.SavedReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SavedReportController {
    private final SavedReportService savedReports;
    private final AccountService accounts;

    public SavedReportController(SavedReportService savedReports, AccountService accounts) {
        this.savedReports = savedReports;
        this.accounts = accounts;
    }

    @GetMapping("/saved")
    public String saved(@AuthenticationPrincipal UserDetails principal, Model model) {
        UserAccount account = accounts.findByEmail(principal.getUsername());
        model.addAttribute("savedReports", savedReports.forAccount(account));
        return "account/saved";
    }

    @PostMapping("/reports/{id}/save")
    public String toggle(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        UserAccount account = accounts.findByEmail(principal.getUsername());
        boolean saved = savedReports.toggle(account, id);
        return saved ? "redirect:/reports/" + id + "?saved" : "redirect:/reports/" + id + "?unsaved";
    }
}
