package com.crm.backend.note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Page<Note> findByCustomerId(Long customerId, Pageable pageable);

    Page<Note> findByLeadId(Long leadId, Pageable pageable);
}