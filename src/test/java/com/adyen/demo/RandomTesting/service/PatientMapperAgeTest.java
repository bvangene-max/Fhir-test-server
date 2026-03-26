package com.adyen.demo.RandomTesting.service;

// Unit tests for PatientMapper age calculation.
// Age is derived from the FHIR birthDate field. These tests cover the
// boundary cases: missing date, valid date, and an invalid future date.

import com.adyen.demo.RandomTesting.entity.InternalPatient;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientMapperAgeTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void toPatientEntity_noBirthDate_ageDefaultsToZero() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/no-dob");

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getAge()).isEqualTo(0);
    }

    @Test
    void toPatientEntity_futureBirthDate_throwsIllegalArgumentException() {
        // A future birth date is impossible — the mapper rejects it so the
        // sync endpoint can return 400 instead of storing a nonsensical age.
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/future-dob");
        fhirPatient.setBirthDate(java.util.Date.from(
                LocalDate.now().plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        assertThatThrownBy(() -> mapper.toPatientEntity(fhirPatient))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Birth date cannot be in the future");
    }
}
