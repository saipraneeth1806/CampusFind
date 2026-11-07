package com.example.campusfind.repository;

import com.example.campusfind.model.ContactThread;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactThreadRepository extends JpaRepository<ContactThread, Long> {
    Optional<ContactThread> findByReport_IdAndRequester_Id(Long reportId, Long requesterId);
    List<ContactThread> findByOwner_IdOrderByUpdatedAtDesc(Long ownerId);
    List<ContactThread> findByRequester_IdOrderByUpdatedAtDesc(Long requesterId);
}
