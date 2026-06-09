package com.nexaerp.journal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findTopByOrderByIdDesc(); //For Last entry number

    boolean existsBySourceTypeAndSourceId(JournalSourceType sourceType, Long sourceId);
}
