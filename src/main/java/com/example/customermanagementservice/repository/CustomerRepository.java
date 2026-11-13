package com.example.customermanagementservice.repository;

import com.example.customermanagementservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByName(String name);

    // citizen_number is unique, so there is at most one match.
    Optional<Customer> findByCitizenNumber(long citizenNumber);

    boolean existsByCitizenNumber(long citizenNumber);
}
