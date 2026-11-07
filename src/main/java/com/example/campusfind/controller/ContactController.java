package com.example.campusfind.controller;

import com.example.campusfind.dto.ContactMessageForm;
import com.example.campusfind.model.ContactThread;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.service.AccountService;
import com.example.campusfind.service.ContactService;
import com.example.campusfind.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ContactController {
    private final ContactService contacts;
    private final AccountService accounts;
    private final ReportService reports;

    public ContactController(ContactService contacts, AccountService accounts, ReportService reports) {
        this.contacts = contacts;
        this.accounts = accounts;
        this.reports = reports;
    }

    @GetMapping("/reports/{id}/contact")
    public String contactForm(@PathVariable Long id, @RequestParam(required = false) Long thread,
                              Model model) {
        model.addAttribute("report", reports.get(id));
        model.addAttribute("threadId", thread);
        model.addAttribute("messageForm", new ContactMessageForm());
        return "contact/start";
    }

    @PostMapping("/reports/{id}/contact")
    public String startContact(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails principal,
                               @Valid @ModelAttribute("messageForm") ContactMessageForm form,
                               BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("report", reports.get(id));
            model.addAttribute("threadId", null);
            return "contact/start";
        }
        UserAccount requester = accounts.findByEmail(principal.getUsername());
        ContactThread thread = contacts.start(id, requester, form);
        return "redirect:/inbox/" + thread.getId();
    }

    @GetMapping("/inbox")
    public String inbox(@AuthenticationPrincipal UserDetails principal, Model model) {
        UserAccount viewer = accounts.findByEmail(principal.getUsername());
        var threads = contacts.inbox(viewer);
        java.util.Map<Long, Long> unreadByThread = new java.util.HashMap<>();
        for (ContactThread thread : threads) unreadByThread.put(thread.getId(), contacts.unreadCount(thread, viewer));
        model.addAttribute("account", viewer);
        model.addAttribute("threads", threads);
        model.addAttribute("unreadByThread", unreadByThread);
        return "contact/inbox";
    }

    @GetMapping("/inbox/{threadId}")
    public String conversation(@PathVariable Long threadId,
                               @AuthenticationPrincipal UserDetails principal, Model model) {
        UserAccount viewer = accounts.findByEmail(principal.getUsername());
        ContactThread thread = contacts.open(threadId, viewer);
        boolean viewerIsOwner = thread.getOwner().getId().equals(viewer.getId());
        model.addAttribute("thread", thread);
        model.addAttribute("messages", contacts.conversation(threadId, viewer));
        model.addAttribute("viewer", viewer);
        model.addAttribute("otherPerson", viewerIsOwner ? thread.getRequester() : thread.getOwner());
        model.addAttribute("messageForm", new ContactMessageForm());
        model.addAttribute("viewerIsOwner", viewerIsOwner);
        return "contact/thread";
    }

    @PostMapping("/inbox/{threadId}/messages")
    public String reply(@PathVariable Long threadId,
                        @AuthenticationPrincipal UserDetails principal,
                        @Valid @ModelAttribute("messageForm") ContactMessageForm form,
                        BindingResult errors, Model model) {
        UserAccount viewer = accounts.findByEmail(principal.getUsername());
        if (errors.hasErrors()) {
            ContactThread thread = contacts.open(threadId, viewer);
            boolean viewerIsOwner = thread.getOwner().getId().equals(viewer.getId());
            model.addAttribute("thread", thread);
            model.addAttribute("messages", contacts.conversation(threadId, viewer));
            model.addAttribute("viewer", viewer);
            model.addAttribute("otherPerson", viewerIsOwner ? thread.getRequester() : thread.getOwner());
            model.addAttribute("viewerIsOwner", viewerIsOwner);
            return "contact/thread";
        }
        contacts.reply(threadId, viewer, form);
        return "redirect:/inbox/" + threadId;
    }
}
