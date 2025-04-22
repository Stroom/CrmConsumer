package com.example.repository;

import com.example.model.Customer;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByExternalId(Long externalId);

    @Transactional
    @Modifying
    void deleteByExternalId(Long externalId);

} 