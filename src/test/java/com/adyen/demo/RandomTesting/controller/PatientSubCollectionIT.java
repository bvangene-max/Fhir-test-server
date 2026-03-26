package com.adyen.demo.RandomTesting.controller;

// Tests the sub-collection endpoints: names, addresses, and phone numbers.
// Each collection has its own add/remove HTTP endpoint.
// Happy paths are tested per collection (distinct routes), error paths only once
// since the controller logic is identical across all three.

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PatientSubCollectionIT extends PatientControllerITBase {

    // --- Names ---

    @Test
    void addName_existingPatient_appendsName() {
        long id = createPatientAndReturnId();

        Map<String, Object> name = Map.of("family", "Extra", "given", "Name", "use", "nickname");
        ResponseEntity<Map> response = restTemplate.postForEntity(url("/patients/{id}/names"), name, Map.class, id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> names = mapList(bodyOf(response), "names");
        assertThat(names).hasSize(2);
        assertThat(names.get(1).get("family")).isEqualTo("Extra");
    }

    @Test
    void addName_responseContainsNewNameFields() {
        long id = createPatientAndReturnId();

        Map<String, Object> name = Map.of("family", "Jones", "given", "Alice", "use", "official");
        ResponseEntity<Map> response = restTemplate.postForEntity(url("/patients/{id}/names"), name, Map.class, id);

        List<Map<String, Object>> names = mapList(bodyOf(response), "names");
        Map<String, Object> added = names.get(names.size() - 1);
        assertThat(added.get("family")).isEqualTo("Jones");
        assertThat(added.get("given")).isEqualTo("Alice");
        assertThat(added.get("use")).isEqualTo("official");
    }

    @Test
    void addName_nonExistentPatient_returns404() {
        expectClientError(HttpStatus.NOT_FOUND,
                () -> restTemplate.postForEntity(url("/patients/{id}/names"),
                        Map.of("family", "Ghost"), Map.class, 999_999L));
    }

    @Test
    void addName_withUnknownField_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.postForEntity(
                url("/patients/{id}/names"),
                Map.of("family", "Extra", "given", "Name", "unknownField", "should fail"),
                Map.class, id));
    }

    @Test
    void addName_withMalformedJson_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.exchange(
                url("/patients/{id}/names"), HttpMethod.POST,
                jsonBody("{\"family\":\"Extra\""), Map.class, id));
    }

    @Test
    void removeName_existingIndex_removesName() {
        long id = createPatientAndReturnId();

        ResponseEntity<Map> response = deleteFromCollection("/patients/{id}/names/{index}", id, 0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> names = mapList(bodyOf(response), "names");
        assertThat(names).isEmpty();
    }

    @Test
    void removeName_nonExistentPatient_returns404() {
        expectClientError(HttpStatus.NOT_FOUND,
                () -> deleteFromCollection("/patients/{id}/names/{index}", 999_999L, 0));
    }

    @Test
    void removeName_outOfBoundsIndex_returns404() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.NOT_FOUND,
                () -> deleteFromCollection("/patients/{id}/names/{index}", id, 99));
    }

    @Test
    void removeName_middleIndex_removesCorrectEntry() {
        long id = createPatientAndReturnId();
        restTemplate.postForEntity(url("/patients/{id}/names"),
                Map.of("family", "Second", "given", "B"), Map.class, id);
        restTemplate.postForEntity(url("/patients/{id}/names"),
                Map.of("family", "Third", "given", "C"), Map.class, id);

        ResponseEntity<Map> response = deleteFromCollection("/patients/{id}/names/{index}", id, 1);

        List<Map<String, Object>> names = mapList(bodyOf(response), "names");
        assertThat(names).hasSize(2);
        assertThat(names.get(0).get("family")).isEqualTo("Tester");
        assertThat(names.get(1).get("family")).isEqualTo("Third");
    }

    // --- Addresses ---

    @Test
    void addAddress_existingPatient_appendsAddress() {
        long id = createPatientAndReturnId();

        Map<String, Object> address = Map.of("line", "456 New St", "city", "Ghent",
                "postalCode", "9000", "country", "BE", "use", "work");
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/patients/{id}/addresses"), address, Map.class, id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> addresses = mapList(bodyOf(response), "addresses");
        assertThat(addresses).hasSize(2);
        assertThat(addresses.get(1).get("city")).isEqualTo("Ghent");
    }

    @Test
    void addAddress_withUnknownField_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.postForEntity(
                url("/patients/{id}/addresses"),
                Map.of(
                        "line", "456 New St",
                        "city", "Ghent",
                        "postalCode", "9000",
                        "country", "BE",
                        "use", "work",
                        "unknownField", "should fail"),
                Map.class, id));
    }

    @Test
    void addAddress_withMalformedJson_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.exchange(
                url("/patients/{id}/addresses"), HttpMethod.POST,
                jsonBody("{\"city\":\"Ghent\""), Map.class, id));
    }

    @Test
    void removeAddress_existingIndex_removesAddress() {
        long id = createPatientAndReturnId();

        ResponseEntity<Map> response = deleteFromCollection("/patients/{id}/addresses/{index}", id, 0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> addresses = mapList(bodyOf(response), "addresses");
        assertThat(addresses).isEmpty();
    }

    // --- Phone numbers ---

    @Test
    void addPhoneNumber_existingPatient_appendsPhoneNumber() {
        long id = createPatientAndReturnId();

        Map<String, Object> phone = Map.of("system", "phone", "value", "0499000000", "use", "mobile");
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/patients/{id}/phone-numbers"), phone, Map.class, id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> phones = mapList(bodyOf(response), "phoneNumbers");
        assertThat(phones).hasSize(1);
        assertThat(phones.get(0).get("value")).isEqualTo("0499000000");
    }

    @Test
    void addPhoneNumber_withUnknownField_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.postForEntity(
                url("/patients/{id}/phone-numbers"),
                Map.of("system", "phone", "value", "0499000000", "use", "mobile", "unknownField", "should fail"),
                Map.class, id));
    }

    @Test
    void addPhoneNumber_withMalformedJson_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.exchange(
                url("/patients/{id}/phone-numbers"), HttpMethod.POST,
                jsonBody("{\"value\":\"0499000000\""), Map.class, id));
    }

    @Test
    void removePhoneNumber_existingIndex_removesPhoneNumber() {
        long id = createPatientAndReturnId();
        restTemplate.postForEntity(url("/patients/{id}/phone-numbers"),
                Map.of("system", "phone", "value", "0499000000", "use", "mobile"), Map.class, id);

        ResponseEntity<Map> response = deleteFromCollection("/patients/{id}/phone-numbers/{index}", id, 0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> phones = mapList(bodyOf(response), "phoneNumbers");
        assertThat(phones).isEmpty();
    }
}
