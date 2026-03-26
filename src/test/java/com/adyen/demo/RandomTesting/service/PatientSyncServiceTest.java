package com.adyen.demo.RandomTesting.service;

// Unit tests for PatientService.syncPatientFromFhir.
// Covers: existing patient update, new patient creation, concurrent duplicate handling,
// and FHIR client error propagation.

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.adyen.demo.RandomTesting.entity.InternalPatient;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientSyncServiceTest extends PatientServiceTestBase {

    @Test
    void syncPatientFromFhir_existingPatient_readsAndPersistsMappedEntity() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/xyz");
        when(fhirClient.read().resource(Patient.class).withId("xyz").execute()).thenReturn(fhirPatient);

        var existingEntity = new InternalPatient();
        when(repository.findByFhirId("xyz")).thenReturn(Optional.of(existingEntity));
        when(repository.save(existingEntity)).thenReturn(existingEntity);

        InternalPatient result = service.syncPatientFromFhir("xyz");

        verify(patientMapper).toPatientEntity(eq(fhirPatient), eq(existingEntity));
        verify(repository).save(existingEntity);
        assertThat(result).isSameAs(existingEntity);
    }

    @Test
    void syncPatientFromFhir_newPatient_createsNewEntity() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/new");
        when(fhirClient.read().resource(Patient.class).withId("new").execute()).thenReturn(fhirPatient);
        when(repository.findByFhirId("new")).thenReturn(Optional.empty());
        var savedEntity = new InternalPatient();
        when(repository.save(any(InternalPatient.class))).thenReturn(savedEntity);

        InternalPatient result = service.syncPatientFromFhir("new");

        verify(patientMapper).toPatientEntity(eq(fhirPatient), any(InternalPatient.class));
        assertThat(result).isSameAs(savedEntity);
    }

    @Test
    void syncPatientFromFhir_dataIntegrityViolation_returnsExistingRow() {
        // Two concurrent syncs for the same fhirId — the second insert hits the unique constraint.
        // The service catches DataIntegrityViolationException and returns the row the first sync inserted.
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/race");
        when(fhirClient.read().resource(Patient.class).withId("race").execute()).thenReturn(fhirPatient);
        when(repository.save(any(InternalPatient.class))).thenThrow(new DataIntegrityViolationException("dup"));
        var existing = new InternalPatient();
        when(repository.findByFhirId("race")).thenReturn(Optional.empty()).thenReturn(Optional.of(existing));

        InternalPatient result = service.syncPatientFromFhir("race");

        assertThat(result).isSameAs(existing);
    }

    @Test
    void syncPatientFromFhir_fhirClientThrows_propagatesResourceNotFoundException() {
        when(fhirClient.read().resource(Patient.class).withId("unknown").execute())
                .thenThrow(new ResourceNotFoundException("Patient not found"));

        assertThatThrownBy(() -> service.syncPatientFromFhir("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
