package com.example.campusfind.service;

import com.example.campusfind.dto.ContactMessageForm;
import com.example.campusfind.exception.ReportNotFoundException;
import com.example.campusfind.model.ContactMessage;
import com.example.campusfind.model.ContactThread;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportStatus;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.ContactMessageRepository;
import com.example.campusfind.repository.ContactThreadRepository;
import com.example.campusfind.repository.ReportRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {
    private final ContactThreadRepository threads;
    private final ContactMessageRepository messages;
    private final ReportRepository reports;
    private final Clock clock;

    public ContactService(ContactThreadRepository threads, ContactMessageRepository messages,
                          ReportRepository reports, Clock clock) {
        this.threads = threads;
        this.messages = messages;
        this.reports = reports;
        this.clock = clock;
    }

    @Transactional
    public ContactThread start(Long reportId, UserAccount requester, ContactMessageForm form) {
        Report report = reports.findById(reportId).orElseThrow(() -> new ReportNotFoundException(reportId));
        if (report.getStatus() != ReportStatus.OPEN) throw new IllegalStateException("This report is already resolved.");
        UserAccount owner = report.getOwner();
        if (owner == null) throw new IllegalStateException("This older report has no account owner to contact.");
        if (owner.getId().equals(requester.getId())) throw new AccessDeniedException("You cannot contact yourself.");

        ContactThread thread = threads.findByReport_IdAndRequester_Id(reportId, requester.getId())
                .orElseGet(() -> threads.save(new ContactThread(report, owner, requester)));
        addMessage(thread, requester, form.getBody());
        return thread;
    }

    @Transactional
    public ContactThread reply(Long threadId, UserAccount sender, ContactMessageForm form) {
        ContactThread thread = findAuthorizedThread(threadId, sender);
        addMessage(thread, sender, form.getBody());
        return thread;
    }

    @Transactional
    public ContactThread open(Long threadId, UserAccount viewer) {
        ContactThread thread = findAuthorizedThread(threadId, viewer);
        List<ContactMessage> unread = messages.findByThread_IdAndSender_IdNotAndReadAtIsNull(threadId, viewer.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        unread.forEach(message -> message.setReadAt(now));
        messages.saveAll(unread);
        return thread;
    }

    @Transactional(readOnly = true)
    public List<ContactThread> inbox(UserAccount account) {
        List<ContactThread> result = new ArrayList<>(threads.findByOwner_IdOrderByUpdatedAtDesc(account.getId()));
        result.addAll(threads.findByRequester_IdOrderByUpdatedAtDesc(account.getId()));
        result.sort(Comparator.comparing(ContactThread::getUpdatedAt).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public List<ContactMessage> conversation(Long threadId, UserAccount viewer) {
        findAuthorizedThread(threadId, viewer);
        return messages.findByThread_IdOrderByCreatedAtAsc(threadId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(ContactThread thread, UserAccount viewer) {
        return messages.countByThread_IdAndSender_IdNotAndReadAtIsNull(thread.getId(), viewer.getId());
    }

    @Transactional(readOnly = true)
    public long totalUnreadMessages(UserAccount viewer) {
        if (viewer == null) return 0;
        return inbox(viewer).stream()
                .mapToLong(thread -> unreadCount(thread, viewer))
                .sum();
    }

    private ContactThread findAuthorizedThread(Long threadId, UserAccount viewer) {
        ContactThread thread = threads.findById(threadId).orElseThrow(() -> new ReportNotFoundException(threadId));
        boolean owner = thread.getOwner().getId().equals(viewer.getId());
        boolean requester = thread.getRequester().getId().equals(viewer.getId());
        if (!owner && !requester) throw new AccessDeniedException("This conversation is private.");
        return thread;
    }

    private void addMessage(ContactThread thread, UserAccount sender, String rawBody) {
        String body = rawBody.trim();
        messages.save(new ContactMessage(thread, sender, body));
        thread.setUpdatedAt(LocalDateTime.now(clock));
        threads.save(thread);
    }
}
