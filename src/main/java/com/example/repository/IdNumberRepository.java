package com.example.repository;

import com.example.model.IdNumber;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface IdNumberRepository extends JpaRepository<IdNumber, Long> {

    Optional<IdNumber> findByExternalId(Long externalId);

    @Transactional
    @Modifying
    void deleteByExternalId(Long externalId);

} 