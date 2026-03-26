package com.adyen.demo.RandomTesting.service;

// Unit tests for PatientMapper scalar field mapping:
// fhirId extraction, name string, age calculation, and name list entries.

import com.adyen.demo.RandomTesting.entity.InternalPatient;
import com.adyen.demo.RandomTesting.entity.patientSubclass.Name;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PatientMapperScalarTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void toPatientEntity_copiesAllScalarAndCollectionFields() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/abc");
        fhirPatient.addName(new HumanName().setFamily("Doe").addGiven("Jane").setUse(HumanName.NameUse.OFFICIAL));
        fhirPatient.setBirthDate(java.util.Date.from(
                LocalDate.of(1990, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getFhirId()).isEqualTo("abc");
        assertThat(entity.getName()).isEqualTo("Jane Doe");
        int expectedAge = Period.between(LocalDate.of(1990, 1, 1), LocalDate.now()).getYears();
        assertThat(entity.getAge()).isEqualTo(expectedAge);

        assertThat(entity.getNames()).hasSize(1);
        Name name = entity.getNames().get(0);
        assertThat(name.getFamily()).isEqualTo("Doe");
        assertThat(name.getGiven()).isEqualTo("Jane");
        assertThat(name.getUse()).isEqualTo("official");
    }

    @Test
    void toPatientEntity_fhirIdWithPrefix_stripsPrefix() {
        // HAPI sets id as "Patient/abc" — getIdPart() returns only "abc"
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/abc");

        assertThat(mapper.toPatientEntity(fhirPatient).getFhirId()).isEqualTo("abc");
    }

    @Test
    void toPatientEntity_fhirIdWithoutPrefix_usedAsIs() {
        var fhirPatient = new Patient();
        fhirPatient.setId("bare-id");

        assertThat(mapper.toPatientEntity(fhirPatient).getFhirId()).isEqualTo("bare-id");
    }

    @Test
    void toPatientEntity_noNames_namesListIsEmptyAndNameIsNull() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/no-name");

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getNames()).isEmpty();
        assertThat(entity.getName()).isNull();
    }

    @Test
    void toPatientEntity_noFamilyName_doesNotThrowAndFamilyIsNull() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/given-only");
        fhirPatient.addName(new HumanName().addGiven("Alice").setUse(HumanName.NameUse.USUAL));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getNames()).hasSize(1);
        assertThat(entity.getNames().get(0).getFamily()).isNull();
        assertThat(entity.getNames().get(0).getGiven()).isEqualTo("Alice");
        assertThat(entity.getName()).isNotBlank();
    }

    @Test
    void toPatientEntity_multipleGivenNames_joinedWithSpace() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/middle-name");
        fhirPatient.addName(new HumanName().setFamily("Doe").addGiven("John").addGiven("Michael").addGiven("James"));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getNames().get(0).getGiven()).isEqualTo("John Michael James");
    }

    @Test
    void toPatientEntity_nullNameUse_useIsNull() {
        var fhirPatient = new Patient();
        fhirPatient.setId("Patient/no-name-use");
        fhirPatient.addName(new HumanName().setFamily("Doe"));

        InternalPatient entity = mapper.toPatientEntity(fhirPatient);

        assertThat(entity.getNames().get(0).getUse()).isNull();
    }
}
