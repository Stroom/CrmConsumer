package com.example.repository;

import com.example.model.CustomerRelation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface CustomerRelationRepository extends JpaRepository<CustomerRelation, Long> {

    Optional<CustomerRelation> findByExternalId(Long externalId);

    @Transactional
    @Modifying
    void deleteByExternalId(Long externalId);

} 