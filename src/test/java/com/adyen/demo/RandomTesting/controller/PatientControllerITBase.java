package com.adyen.demo.RandomTesting.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import com.adyen.demo.RandomTesting.repository.PatientRepository;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class PatientControllerITBase {

    protected final RestTemplate restTemplate = new RestTemplate(
            new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());

    @LocalServerPort
    protected int port;

    @Autowired
    protected PatientRepository patientRepository;

    @MockitoBean
    protected IGenericClient fhirClient;

    @BeforeEach
    void cleanDatabase() {
        patientRepository.deleteAll();
    }

    protected Map<String, Object> buildPatientPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Integration Tester");
        payload.put("age", 37);
        payload.put("names", List.of(Map.of("family", "Tester", "given", "Integration", "use", "official")));
        payload.put("addresses", List.of(Map.of(
                "line", "123 Integration Way",
                "city", "Brussels",
                "postalCode", "1000",
                "country", "BE",
                "use", "home")));
        return payload;
    }

    protected Patient buildFhirPatient(String fhirId) {
        var fhirPatient = new Patient();
        fhirPatient.setId(fhirId);
        fhirPatient.addName().setFamily("Smith").addGiven("John");
        fhirPatient.setBirthDate(java.sql.Date.valueOf("1990-01-01"));
        return fhirPatient;
    }

    @SuppressWarnings("unchecked")
    protected void stubFhirRead(String fhirId, Patient fhirPatient) {
        IRead readOp = mock(IRead.class);
        IReadTyped<Patient> readTyped = mock(IReadTyped.class);
        IReadExecutable<Patient> readExec = mock(IReadExecutable.class);
        when(fhirClient.read()).thenReturn(readOp);
        when(readOp.resource(Patient.class)).thenReturn(readTyped);
        when(readTyped.withId(fhirId)).thenReturn(readExec);
        when(readExec.execute()).thenReturn(fhirPatient);
    }

    protected ResponseEntity<Map> createPatient() {
        return createPatient(buildPatientPayload());
    }

    protected ResponseEntity<Map> createPatient(Map<String, Object> payload) {
        return restTemplate.postForEntity(url("/patients"), payload, Map.class);
    }

    protected long createPatientAndReturnId() {
        return idOf(createPatient());
    }

    protected long idOf(ResponseEntity<Map> response) {
        return ((Number) bodyOf(response).get("id")).longValue();
    }

    protected Map<String, Object> bodyOf(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    protected ResponseEntity<Map> patchPatient(long id, Map<String, Object> patch) {
        return restTemplate.exchange(url("/patients/{id}"), HttpMethod.PATCH, new HttpEntity<>(patch), Map.class, id);
    }

    protected ResponseEntity<Map> deleteFromCollection(String path, long id, int index) {
        return restTemplate.exchange(url(path), HttpMethod.DELETE, null, Map.class, id, index);
    }

    protected HttpEntity<String> jsonBody(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> mapList(Map<String, Object> body, String field) {
        return (List<Map<String, Object>>) body.get(field);
    }

    protected HttpClientErrorException expectClientError(HttpStatus status, ThrowingHttpCall call) {
        HttpClientErrorException exception = catchThrowableOfType(call::run, HttpClientErrorException.class);
        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode()).isEqualTo(status);
        return exception;
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    @FunctionalInterface
    protected interface ThrowingHttpCall {
        void run();
    }
}
