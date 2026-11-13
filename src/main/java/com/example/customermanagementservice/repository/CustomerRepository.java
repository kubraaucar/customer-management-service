package com.example.customermanagementservice.repository;

import com.example.customermanagementservice.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    // Matches the name, the surname or "name surname", ignoring upper/lower case.
    @Query("""
            select c from Customer c
            where lower(concat(c.name, ' ', c.surname)) like lower(concat('%', :search, '%'))
            """)
    Page<Customer> search(@Param("search") String search, Pageable pageable);

    boolean existsByCitizenNumber(long citizenNumber);
}
