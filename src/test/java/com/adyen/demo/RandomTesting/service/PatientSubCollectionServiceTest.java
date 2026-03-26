package com.adyen.demo.RandomTesting.service;

// Unit tests for PatientService sub-collection operations: addName and removeName.
// Address and phone variants are structurally identical code paths, so only name
// variants are tested here — a bug in the shared logic would be caught by these.

import com.adyen.demo.RandomTesting.entity.InternalPatient;
import com.adyen.demo.RandomTesting.entity.patientSubclass.Name;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientSubCollectionServiceTest extends PatientServiceTestBase {

    @Test
    void addName_patientExists_addsNameAndSaves() {
        var patient = new InternalPatient();
        var name = new Name();
        when(repository.findById(1L)).thenReturn(Optional.of(patient));
        when(repository.save(patient)).thenReturn(patient);

        InternalPatient result = service.addName(1L, name);

        assertThat(result).isSameAs(patient);
        assertThat(patient.getNames()).contains(name);
        verify(repository).save(patient);
    }

    @Test
    void addName_patientNotFound_returnsNull() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.addName(99L, new Name())).isNull();
    }

    @Test
    void removeName_patientExists_removesNameAndSaves() {
        var patient = new InternalPatient();
        patient.getNames().add(new Name());
        when(repository.findById(1L)).thenReturn(Optional.of(patient));
        when(repository.save(patient)).thenReturn(patient);

        InternalPatient result = service.removeName(1L, 0);

        assertThat(result).isSameAs(patient);
        assertThat(patient.getNames()).isEmpty();
    }

    @Test
    void removeName_patientNotFound_returnsNull() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.removeName(99L, 0)).isNull();
    }
}
