package com.example.campusfind.controller;

import com.example.campusfind.model.UserAccount;
import com.example.campusfind.service.AccountService;
import com.example.campusfind.service.MatchAlertService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MatchAlertController {
    private final MatchAlertService alerts;
    private final AccountService accounts;

    public MatchAlertController(MatchAlertService alerts, AccountService accounts) {
        this.alerts = alerts;
        this.accounts = accounts;
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails principal, Model model) {
        UserAccount account = accounts.findByEmail(principal.getUsername());
        model.addAttribute("alerts", alerts.forAccount(account));
        model.addAttribute("unreadCount", alerts.unreadCount(account));
        model.addAttribute("account", account);
        return "account/notifications";
    }

    @GetMapping("/notifications/{id}")
    public String open(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        UserAccount account = accounts.findByEmail(principal.getUsername());
        var alert = alerts.open(id, account);
        return "redirect:/reports/" + alert.getNewReport().getId();
    }
}
