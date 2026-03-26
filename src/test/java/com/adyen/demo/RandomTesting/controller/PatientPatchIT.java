package com.adyen.demo.RandomTesting.controller;

// Tests PATCH-specific behavior: partial updates, field protection, and list replacement.
// Validation rules for PATCH live in PatientValidationIT.

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PatientPatchIT extends PatientControllerITBase {

    @Test
    void patchPatient_updatesOnlyProvidedFields() {
        long id = createPatientAndReturnId();

        ResponseEntity<Map> response = patchPatient(id, Map.of("name", "Updated Name"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bodyOf(response).get("name")).isEqualTo("Updated Name");
        assertThat(bodyOf(response).get("age")).isEqualTo(37); // unchanged
    }

    @Test
    void patchPatient_nonExistentId_returns404() {
        expectClientError(HttpStatus.NOT_FOUND,
                () -> patchPatient(999_999L, Map.of("name", "Ghost")));
    }

    @Test
    // id in the PATCH body must be ignored — the path variable is always authoritative.
    // Prevents a caller from accidentally reassigning a patient to a different id.
    void patchPatient_idInBody_isIgnoredAndPathIdUsed() {
        long id = createPatientAndReturnId();

        Map<String, Object> patch = new HashMap<>();
        patch.put("name", "Renamed");
        patch.put("id", 99999);

        ResponseEntity<Map> response = patchPatient(id, patch);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idOf(response)).isEqualTo(id);
    }

    @Test
    void patchPatient_replacesNamesList() {
        long id = createPatientAndReturnId();

        Map<String, Object> patch = Map.of("names", List.of(
                Map.of("family", "Replaced", "given", "Name", "use", "official")));
        ResponseEntity<Map> response = patchPatient(id, patch);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> names = mapList(bodyOf(response), "names");
        assertThat(names).hasSize(1);
        assertThat(names.get(0).get("family")).isEqualTo("Replaced");
    }

    @Test
    void patchPatient_withEmptyNamesList_clearsNames() {
        long id = createPatientAndReturnId();

        Map<String, Object> patch = new HashMap<>();
        patch.put("names", List.of());
        ResponseEntity<Map> response = patchPatient(id, patch);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> names = (List<?>) bodyOf(response).get("names");
        assertThat(names).isEmpty();
    }

    @Test
    void patchPatient_withEmptyAddressesList_clearsAddresses() {
        long id = createPatientAndReturnId();

        Map<String, Object> patch = new HashMap<>();
        patch.put("addresses", List.of());
        ResponseEntity<Map> response = patchPatient(id, patch);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> addresses = (List<?>) bodyOf(response).get("addresses");
        assertThat(addresses).isEmpty();
    }
}
