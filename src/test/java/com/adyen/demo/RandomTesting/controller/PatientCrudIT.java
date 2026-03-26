package com.adyen.demo.RandomTesting.controller;

// Tests basic CRUD operations: create, read, list, delete.
// These cover the core happy paths and fundamental error cases (404, empty list).

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PatientCrudIT extends PatientControllerITBase {

    @Test
    void createAndRetrievePatient() {
        Map<String, Object> payload = buildPatientPayload();

        ResponseEntity<Map> createResponse = createPatient(payload);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> created = bodyOf(createResponse);
        long createdId = idOf(createResponse);
        assertThat(created.get("name")).isEqualTo(payload.get("name"));

        ResponseEntity<Map> getResponse = restTemplate.getForEntity(url("/patients/{id}"), Map.class, createdId);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> retrieved = bodyOf(getResponse);
        assertThat(retrieved.get("name")).isEqualTo(payload.get("name"));

        List<Map<String, Object>> names = mapList(retrieved, "names");
        assertThat(names).hasSize(1);
        assertThat(names.get(0).get("family")).isEqualTo("Tester");
        assertThat(names.get(0).get("given")).isEqualTo("Integration");
    }

    @Test
    void createPatient_withoutIdInBody_returnsGeneratedId() {
        ResponseEntity<Map> response = createPatient();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(idOf(response)).isGreaterThan(0);
    }

    @Test
    void createPatient_fhirIdIsNullInResponse() {
        // fhirId is only populated via the sync endpoint, never on manual create
        ResponseEntity<Map> response = createPatient();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bodyOf(response)).containsKey("fhirId");
        assertThat(bodyOf(response).get("fhirId")).isNull();
    }

    @Test
    void createPatient_withNullName_returns400() {
        // name is @NotBlank — omitting it is rejected
        Map<String, Object> payload = buildPatientPayload();
        payload.remove("name");

        expectClientError(HttpStatus.BAD_REQUEST, () -> createPatient(payload));
    }

    @Test
    void getAllPatients_emptyDatabase_returnsEmptyList() {
        ResponseEntity<List> response = restTemplate.getForEntity(url("/patients"), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getAllPatients_multiplePatients_returnsAll() {
        createPatient();
        createPatient();

        ResponseEntity<List> response = restTemplate.getForEntity(url("/patients"), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getAllPatients_includesSubCollectionFields() {
        createPatient();

        ResponseEntity<List> response = restTemplate.getForEntity(url("/patients"), List.class);

        Map<String, Object> patient = (Map<String, Object>) response.getBody().get(0);
        List<Map<String, Object>> names = mapList(patient, "names");
        assertThat(names).hasSize(1);
        assertThat(names.get(0).get("family")).isEqualTo("Tester");
        List<Map<String, Object>> addresses = mapList(patient, "addresses");
        assertThat(addresses).hasSize(1);
        assertThat(addresses.get(0).get("city")).isEqualTo("Brussels");
    }

    @Test
    void getPatient_nonExistentId_returns404() {
        expectClientError(HttpStatus.NOT_FOUND,
                () -> restTemplate.getForEntity(url("/patients/{id}"), Map.class, 999_999L));
    }

    @Test
    void getPatient_returnsAllAddressFields() {
        long id = createPatientAndReturnId();

        ResponseEntity<Map> response = restTemplate.getForEntity(url("/patients/{id}"), Map.class, id);

        List<Map<String, Object>> addresses = mapList(bodyOf(response), "addresses");
        Map<String, Object> addr = addresses.get(0);
        assertThat(addr.get("line")).isEqualTo("123 Integration Way");
        assertThat(addr.get("city")).isEqualTo("Brussels");
        assertThat(addr.get("postalCode")).isEqualTo("1000");
        assertThat(addr.get("country")).isEqualTo("BE");
        assertThat(addr.get("use")).isEqualTo("home");
    }

    @Test
    void deletePatient_existingPatient_thenGetReturnsNull() {
        long id = createPatientAndReturnId();

        restTemplate.delete(url("/patients/{id}"), id);

        expectClientError(HttpStatus.NOT_FOUND,
                () -> restTemplate.getForEntity(url("/patients/{id}"), Map.class, id));
    }

    @Test
    void deletePatient_nonExistentId_returns2xx() {
        // Spring Data deleteById on a missing row does nothing — controller returns 200 silently
        restTemplate.delete(url("/patients/{id}"), 999_999L);
        // No exception means the server did not blow up with 5xx
    }
}
