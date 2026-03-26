package com.adyen.demo.RandomTesting.repository;

import com.adyen.demo.RandomTesting.entity.InternalPatient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<InternalPatient, Long> {
    Optional<InternalPatient> findByFhirId(String fhirId);
}