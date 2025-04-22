package com.example.repository;

import com.example.model.IdDocument;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface IdDocumentRepository extends JpaRepository<IdDocument, Long> {

    Optional<IdDocument> findByExternalId(Long externalId);

    @Transactional
    @Modifying
    void deleteByExternalId(Long externalId);

} 