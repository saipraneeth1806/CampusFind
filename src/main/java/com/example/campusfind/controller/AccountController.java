package com.example.campusfind.controller;

import com.example.campusfind.dto.RegistrationForm;
import com.example.campusfind.model.ContactThread;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.service.AccountService;
import com.example.campusfind.service.ContactService;
import com.example.campusfind.service.ReportService;
import com.example.campusfind.service.MatchAlertService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AccountController {
    private final AccountService accounts;
    private final ReportService reports;
    private final ContactService contacts;
    private final MatchAlertService alerts;

    public AccountController(AccountService accounts, ReportService reports, ContactService contacts, MatchAlertService alerts) {
        this.accounts = accounts;
        this.reports = reports;
        this.contacts = contacts;
        this.alerts = alerts;
    }

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                           BindingResult errors, Model model) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch", "The passwords do not match.");
        }
        if (errors.hasErrors()) return "auth/register";
        try {
            accounts.register(form);
        } catch (AccountService.EmailAlreadyRegisteredException duplicate) {
            errors.rejectValue("email", "email.taken", "An account already uses this email.");
            return "auth/register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/account")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        UserAccount account = accounts.findByEmail(principal.getUsername());
        var threads = contacts.inbox(account);
        Map<Long, Long> unreadByThread = new LinkedHashMap<>();
        long unreadTotal = 0;
        for (ContactThread thread : threads) {
            long unread = contacts.unreadCount(thread, account);
            unreadByThread.put(thread.getId(), unread);
            unreadTotal += unread;
        }
        model.addAttribute("account", account);
        model.addAttribute("myReports", reports.findOwnedBy(account));
        model.addAttribute("recentThreads", threads.stream().limit(4).toList());
        model.addAttribute("unreadByThread", unreadByThread);
        model.addAttribute("unreadTotal", unreadTotal);
        model.addAttribute("unreadAlertTotal", alerts.unreadCount(account));
        return "account/dashboard";
    }
}
