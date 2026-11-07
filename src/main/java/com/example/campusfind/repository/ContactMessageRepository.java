package com.example.campusfind.repository;

import com.example.campusfind.model.ContactMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findByThread_IdOrderByCreatedAtAsc(Long threadId);
    List<ContactMessage> findByThread_IdAndSender_IdNotAndReadAtIsNull(Long threadId, Long senderId);
    long countByThread_IdAndSender_IdNotAndReadAtIsNull(Long threadId, Long senderId);
}
