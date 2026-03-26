package com.adyen.demo.RandomTesting.controller;

// Tests optimistic locking version field behavior.
// The @Version field protects against lost updates in concurrent scenarios.
// These tests verify the version is exposed in responses and increments correctly.

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PatientVersionIT extends PatientControllerITBase {

    @Test
    void createPatient_responseExposesVersionField() {
        ResponseEntity<Map> response = createPatient();

        assertThat(bodyOf(response)).containsKey("version");
        assertThat(((Number) bodyOf(response).get("version")).longValue()).isEqualTo(0L);
    }

    @Test
    void getAllPatients_responseExposesVersionField() {
        createPatient();

        ResponseEntity<List> response = restTemplate.getForEntity(url("/patients"), List.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> patient = (Map<String, Object>) response.getBody().get(0);
        assertThat(patient).containsKey("version");
        assertThat(((Number) patient.get("version")).longValue()).isEqualTo(0L);
    }

    @Test
    void patchPatient_versionIncrementsAfterUpdate() {
        ResponseEntity<Map> created = createPatient();
        long id = idOf(created);
        long initialVersion = ((Number) bodyOf(created).get("version")).longValue();

        ResponseEntity<Map> patched = patchPatient(id, Map.of("name", "Updated"));

        assertThat(((Number) bodyOf(patched).get("version")).longValue()).isGreaterThan(initialVersion);
    }

    @Test
    void getPatient_afterPatch_returnsIncrementedVersion() {
        ResponseEntity<Map> created = createPatient();
        long id = idOf(created);
        long initialVersion = ((Number) bodyOf(created).get("version")).longValue();

        patchPatient(id, Map.of("name", "Updated"));

        ResponseEntity<Map> response = restTemplate.getForEntity(url("/patients/{id}"), Map.class, id);

        assertThat(((Number) bodyOf(response).get("version")).longValue()).isGreaterThan(initialVersion);
    }

    @Test
    void addName_incrementsVersion() {
        ResponseEntity<Map> created = createPatient();
        long id = idOf(created);
        long initialVersion = ((Number) bodyOf(created).get("version")).longValue();

        ResponseEntity<Map> response = restTemplate.postForEntity(url("/patients/{id}/names"),
                Map.of("family", "Extra", "given", "Name"), Map.class, id);

        assertThat(((Number) bodyOf(response).get("version")).longValue()).isGreaterThan(initialVersion);
    }
}
