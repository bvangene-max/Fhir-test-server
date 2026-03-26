package com.adyen.demo.RandomTesting.service;

// Unit tests for basic CRUD operations on PatientService:
// get by id, get all, save, and delete.

import com.adyen.demo.RandomTesting.entity.InternalPatient;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientCrudServiceTest extends PatientServiceTestBase {

    @Test
    void getPatientByID_found_returnsPatient() {
        var patient = new InternalPatient();
        when(repository.findById(1L)).thenReturn(Optional.of(patient));

        assertThat(service.getPatientByID(1L)).isSameAs(patient);
    }

    @Test
    void getPatientByID_notFound_returnsNull() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.getPatientByID(99L)).isNull();
    }

    @Test
    void getAllPatients_emptyRepository_returnsEmptyList() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        assertThat(service.getAllPatients()).isEmpty();
    }

    @Test
    void savePatient_delegatesToRepository() {
        var patient = new InternalPatient();
        when(repository.save(patient)).thenReturn(patient);

        InternalPatient result = service.savePatient(patient);

        verify(repository).save(patient);
        assertThat(result).isSameAs(patient);
    }

    @Test
    void deletePatientByID_callsRepositoryDeleteById() {
        service.deletePatientByID(1L);

        verify(repository).deleteById(1L);
    }
}
