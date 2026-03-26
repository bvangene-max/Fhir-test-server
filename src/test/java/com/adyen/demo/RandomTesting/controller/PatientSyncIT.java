package com.adyen.demo.RandomTesting.controller;

// Tests the FHIR sync endpoint: POST /patients/sync/{fhirId}.
// Covers successful sync, idempotency, data mapping, error propagation from
// the external FHIR server, and edge cases like future birth dates.

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientSyncIT extends PatientControllerITBase {

    @Test
    void syncPatient_newFhirId_createsOneRow() {
        stubFhirRead("fhir-001", buildFhirPatient("fhir-001"));

        ResponseEntity<Map> response = restTemplate.postForEntity(url("/patients/sync/fhir-001"), null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patientRepository.findByFhirId("fhir-001")).isPresent();
        assertThat(patientRepository.count()).isEqualTo(1);
    }

    @Test
    void syncPatient_sameFhirIdTwice_doesNotCreateDuplicate() {
        stubFhirRead("fhir-002", buildFhirPatient("fhir-002"));

        restTemplate.postForEntity(url("/patients/sync/fhir-002"), null, Map.class);
        restTemplate.postForEntity(url("/patients/sync/fhir-002"), null, Map.class);

        long count = patientRepository.findAll().stream()
                .filter(p -> "fhir-002".equals(p.getFhirId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void syncPatient_sameFhirId_updatesExistingPatientData() {
        Patient v1 = buildFhirPatient("fhir-update");
        v1.getNameFirstRep().setFamily("Original");
        stubFhirRead("fhir-update", v1);
        restTemplate.postForEntity(url("/patients/sync/fhir-update"), null, Map.class);

        Patient v2 = buildFhirPatient("fhir-update");
        v2.getNameFirstRep().setFamily("Updated");
        stubFhirRead("fhir-update", v2);
        ResponseEntity<Map> response = restTemplate.postForEntity(url("/patients/sync/fhir-update"), null, Map.class);

        assertThat(patientRepository.count()).isEqualTo(1);
        assertThat(response.getBody().get("name")).asString().contains("Updated");
    }

    @Test
    void syncPatient_responseContainsFhirId() {
        stubFhirRead("fhir-check-id", buildFhirPatient("fhir-check-id"));

        ResponseEntity<Map> response = restTemplate.postForEntity(url("/patients/sync/fhir-check-id"), null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bodyOf(response).get("fhirId")).isEqualTo("fhir-check-id");
    }

    @Test
    void syncPatient_reSyncRetainsFhirId() {
        stubFhirRead("fhir-retain-id", buildFhirPatient("fhir-retain-id"));
        restTemplate.postForEntity(url("/patients/sync/fhir-retain-id"), null, Map.class);

        stubFhirRead("fhir-retain-id", buildFhirPatient("fhir-retain-id"));
        ResponseEntity<Map> response = restTemplate.postForEntity(url("/patients/sync/fhir-retain-id"), null, Map.class);

        assertThat(bodyOf(response).get("fhirId")).isEqualTo("fhir-retain-id");
        assertThat(patientRepository.count()).isEqualTo(1);
    }

    @Test
    void syncPatient_responseIncludesSubCollectionFields() {
        Patient fhirPatient = new Patient();
        fhirPatient.setId("fhir-subcol");
        fhirPatient.addName().setFamily("Sync").addGiven("Person");
        fhirPatient.addAddress(new Address()
                .addLine("1 Sync St").setCity("Namur").setPostalCode("5000").setCountry("BE")
                .setUse(Address.AddressUse.HOME));
        fhirPatient.addTelecom(new org.hl7.fhir.r4.model.ContactPoint()
                .setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE)
                .setValue("+3281000000")
                .setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.WORK));
        fhirPatient.setBirthDate(java.sql.Date.valueOf("1985-06-15"));
        stubFhirRead("fhir-subcol", fhirPatient);

        ResponseEntity<Map> response = restTemplate.postForEntity(url("/patients/sync/fhir-subcol"), null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = bodyOf(response);
        List<Map<String, Object>> names = mapList(body, "names");
        assertThat(names).hasSize(1);
        assertThat(names.get(0).get("family")).isEqualTo("Sync");
        List<Map<String, Object>> addresses = mapList(body, "addresses");
        assertThat(addresses).hasSize(1);
        assertThat(addresses.get(0).get("city")).isEqualTo("Namur");
        List<Map<String, Object>> phones = mapList(body, "phoneNumbers");
        assertThat(phones).hasSize(1);
        assertThat(phones.get(0).get("value")).isEqualTo("+3281000000");
    }

    @Test
    void syncPatient_futureBirthDate_returns400() {
        Patient fhirPatient = new Patient();
        fhirPatient.setId("fhir-future-dob");
        fhirPatient.addName().setFamily("Future").addGiven("Person");
        fhirPatient.setBirthDate(java.sql.Date.valueOf(java.time.LocalDate.now().plusYears(1)));
        stubFhirRead("fhir-future-dob", fhirPatient);

        expectClientError(HttpStatus.BAD_REQUEST,
                () -> restTemplate.postForEntity(url("/patients/sync/fhir-future-dob"), null, Map.class));
    }

    @Test
    void syncPatient_fhirPatientNotFound_returns404() {
        IRead readOp = mock(IRead.class);
        IReadTyped<Patient> readTyped = mock(IReadTyped.class);
        when(fhirClient.read()).thenReturn(readOp);
        when(readOp.resource(Patient.class)).thenReturn(readTyped);
        when(readTyped.withId("fhir-missing")).thenThrow(new ResourceNotFoundException("not found"));

        expectClientError(HttpStatus.NOT_FOUND,
                () -> restTemplate.postForEntity(url("/patients/sync/fhir-missing"), null, Map.class));
    }

    @Test
    void syncPatient_fhirServerUnreachable_returns502() {
        IRead readOp = mock(IRead.class);
        when(fhirClient.read()).thenReturn(readOp);
        when(readOp.resource(Patient.class))
                .thenThrow(new FhirClientConnectionException("Connection refused"));

        try {
            restTemplate.postForEntity(url("/patients/sync/fhir-999"), null, Map.class);
            org.junit.jupiter.api.Assertions.fail("Expected 502");
        } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            assertThat(e.getStatusCode().value()).isEqualTo(502);
        }
    }

    @Test
    void syncPatient_fhirServerAuthFailure_returns502() {
        IRead readOp = mock(IRead.class);
        when(fhirClient.read()).thenReturn(readOp);
        when(readOp.resource(Patient.class))
                .thenThrow(new AuthenticationException("Unauthorized"));

        try {
            restTemplate.postForEntity(url("/patients/sync/fhir-998"), null, Map.class);
            org.junit.jupiter.api.Assertions.fail("Expected 502");
        } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            assertThat(e.getStatusCode().value()).isEqualTo(502);
        }
    }
}
