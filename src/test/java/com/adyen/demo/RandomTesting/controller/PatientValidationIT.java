package com.adyen.demo.RandomTesting.controller;

// Tests input validation rules enforced by the API.
// Covers age boundaries (@Min(0), @Max(150)), type coercion edge cases,
// and that server-controlled fields (id) cannot be set by callers.

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PatientValidationIT extends PatientControllerITBase {

    @Test
    // Only age provided — name is @NotBlank so this must be rejected
    void createPatient_withOnlyAge_returns400() {
        expectClientError(HttpStatus.BAD_REQUEST,
                () -> createPatient(Map.of("age", 25)));
    }

    @Test
    // name + age is the minimum valid payload — all list fields default to empty
    void createPatient_withOnlyNameAndAge_succeeds() {
        ResponseEntity<Map> response = createPatient(Map.of("name", "Minimal", "age", 25));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bodyOf(response).get("name")).isEqualTo("Minimal");
        assertThat(((Number) bodyOf(response).get("age")).intValue()).isEqualTo(25);
        assertThat(mapList(bodyOf(response), "names")).isEmpty();
        assertThat(mapList(bodyOf(response), "addresses")).isEmpty();
        assertThat(mapList(bodyOf(response), "phoneNumbers")).isEmpty();
    }

    @Test
    void createPatient_withMissingAge_returns400() {
        Map<String, Object> payload = buildPatientPayload();
        payload.remove("age");

        expectClientError(HttpStatus.BAD_REQUEST, () -> createPatient(payload));
    }

    @Test
    // Age must be >= 0. A negative value is nonsensical and should be rejected
    // immediately so bad data never reaches the database.
    void createPatient_withNegativeAge_returns400() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("age", -1);

        expectClientError(HttpStatus.BAD_REQUEST, () -> createPatient(payload));
    }

    @Test
    // Age 0 is the lower boundary and must be accepted (newborns are valid patients).
    void createPatient_withAgeZero_succeeds() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("age", 0);

        ResponseEntity<Map> response = createPatient(payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((Number) bodyOf(response).get("age")).intValue()).isEqualTo(0);
    }

    @Test
    // 150 is the upper boundary and must be accepted. It is intentionally high
    // enough that no real patient will reach it, but low enough to catch absurd
    // values like data-entry errors or unit mismatches (e.g. age stored in months).
    void createPatient_withAge150_succeeds() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("age", 150);

        ResponseEntity<Map> response = createPatient(payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((Number) bodyOf(response).get("age")).intValue()).isEqualTo(150);
    }

    @Test
    // 151 is one past the cap and must be rejected. This pins the exact boundary:
    // if @Max(150) is ever loosened or removed, this test will fail immediately.
    void createPatient_withAge151_returns400() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("age", 151);

        expectClientError(HttpStatus.BAD_REQUEST, () -> createPatient(payload));
    }

    @Test
    // Jackson silently truncates decimals to int (3.7 → 3) rather than rejecting them.
    // This test documents that chosen behavior — if we ever want to reject decimals
    // we would need a custom deserializer, and this test would need to change to expect 400.
    void createPatient_withDecimalAge_truncatesToInt() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("age", 3.7);

        ResponseEntity<Map> response = createPatient(payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((Number) bodyOf(response).get("age")).intValue()).isEqualTo(3);
    }

    @Test
    // "abc" cannot be coerced to int — Jackson should return 400
    void createPatient_withStringAge_returns400() {
        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.exchange(
                url("/patients"), HttpMethod.POST,
                jsonBody("{\"name\":\"Test\",\"age\":\"abc\"}"), Map.class));
    }

    @Test
    // With strict unknown-field rejection, an unexpected id in the body is a client error.
    void createPatient_withIdInBody_returns400() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("id", 99);

        expectClientError(HttpStatus.BAD_REQUEST, () -> createPatient(payload));
    }

    @Test
    void createPatient_withUnknownField_returns400() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("unknownField", "should fail");

        expectClientError(HttpStatus.BAD_REQUEST, () -> createPatient(payload));
    }

    @Test
    void createPatient_withBlankName_returns400() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("name", "");

        expectClientError(HttpStatus.BAD_REQUEST, () -> createPatient(payload));
    }

    @Test
    void createPatient_withUnknownFieldInsideName_returns400() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("names", List.of(Map.of(
                "family", "Tester",
                "given", "Integration",
                "use", "official",
                "unknownField", "should fail")));

        expectClientError(HttpStatus.BAD_REQUEST, () -> createPatient(payload));
    }

    @Test
    void createPatient_withInvalidNestedFieldType_returns400() {
        Map<String, Object> payload = buildPatientPayload();
        payload.put("phoneNumbers", List.of(Map.of(
                "system", "phone",
                "value", List.of("not", "a", "string"),
                "use", "mobile")));

        expectClientError(HttpStatus.BAD_REQUEST, () -> createPatient(payload));
    }

    @Test
    void createPatient_withEmptyBody_returns400() {
        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.exchange(
                url("/patients"), HttpMethod.POST, jsonBody(""), Map.class));
    }

    @Test
    void createPatient_withMalformedJson_returns400() {
        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.exchange(
                url("/patients"), HttpMethod.POST,
                jsonBody("{\"name\":\"Test\",\"age\":37"), Map.class));
    }

    @Test
    // Same lower-boundary rule applies on PATCH as on POST — validation runs
    // on the merged entity, not just on create.
    void patchPatient_withNegativeAge_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> patchPatient(id, Map.of("age", -1)));
    }

    @Test
    // Same upper-boundary rule applies on PATCH — validation runs on the merged entity.
    void patchPatient_withAgeAbove150_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> patchPatient(id, Map.of("age", 151)));
    }

    @Test
    void patchPatient_withStringAge_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.exchange(
                url("/patients/{id}"), HttpMethod.PATCH,
                jsonBody("{\"age\":\"abc\"}"), Map.class, id));
    }

    @Test
    void patchPatient_withUnknownField_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> patchPatient(id, Map.of("unknownField", "should fail")));
    }

    @Test
    void patchPatient_withUnknownFieldInsideAddress_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> patchPatient(id, Map.of(
                "addresses", List.of(Map.of(
                        "line", "123 Integration Way",
                        "city", "Brussels",
                        "postalCode", "1000",
                        "country", "BE",
                        "use", "home",
                        "unknownField", "should fail")))));
    }

    @Test
    void patchPatient_withEmptyBody_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.exchange(
                url("/patients/{id}"), HttpMethod.PATCH, jsonBody(""), Map.class, id));
    }

    @Test
    void patchPatient_withMalformedJson_returns400() {
        long id = createPatientAndReturnId();

        expectClientError(HttpStatus.BAD_REQUEST, () -> restTemplate.exchange(
                url("/patients/{id}"), HttpMethod.PATCH,
                jsonBody("{\"age\":37"), Map.class, id));
    }
}
