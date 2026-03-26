package com.adyen.demo.RandomTesting.controller;

// Tests concurrent access scenarios.
// Verifies that the server handles simultaneous requests safely using optimistic
// locking (@Version) and server-side retry logic, without data corruption or 5xx errors.

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PatientConcurrencyIT extends PatientControllerITBase {

    @Test
    void createPatient_concurrentIndependentCreates_allSucceed() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    restTemplate.postForEntity(url("/patients"), buildPatientPayload(), Map.class);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(patientRepository.count()).isEqualTo(threadCount);
    }

    @Test
    void patchPatient_concurrent_returnsConflictOrOk() throws InterruptedException {
        ResponseEntity<Map> created = restTemplate.postForEntity(url("/patients"), buildPatientPayload(), Map.class);
        long id = ((Number) created.getBody().get("id")).longValue();

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Integer> statusCodes = new java.util.concurrent.CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int age = 20 + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ResponseEntity<Map> response = restTemplate.exchange(
                            url("/patients/{id}"), HttpMethod.PATCH,
                            new HttpEntity<>(Map.of("age", age)), Map.class, id);
                    statusCodes.add(response.getStatusCode().value());
                } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
                    statusCodes.add(e.getStatusCode().value());
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(statusCodes).hasSize(threadCount);
        assertThat(statusCodes).allSatisfy(code -> assertThat(code).isIn(200, 409));
        assertThat(statusCodes).contains(200);
    }

    @Test
    void addName_concurrent_returnsConflictOrOk() throws InterruptedException {
        ResponseEntity<Map> created = restTemplate.postForEntity(url("/patients"), buildPatientPayload(), Map.class);
        long id = ((Number) created.getBody().get("id")).longValue();

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Integer> statusCodes = new java.util.concurrent.CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            url("/patients/{id}/names"),
                            Map.of("family", "Family" + idx, "given", "Given" + idx),
                            Map.class, id);
                    statusCodes.add(response.getStatusCode().value());
                } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
                    statusCodes.add(e.getStatusCode().value());
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(statusCodes).hasSize(threadCount);
        assertThat(statusCodes).allSatisfy(code -> assertThat(code).isIn(200, 409));
        assertThat(statusCodes).contains(200);
    }

    @Test
    void removeName_concurrent_returnsConflictOrNotFoundOrOk() throws InterruptedException {
        ResponseEntity<Map> created = restTemplate.postForEntity(url("/patients"), buildPatientPayload(), Map.class);
        long id = ((Number) created.getBody().get("id")).longValue();

        int threadCount = 5;
        for (int i = 0; i < threadCount; i++) {
            restTemplate.postForEntity(url("/patients/{id}/names"),
                    Map.of("family", "Family" + i, "given", "Given" + i), Map.class, id);
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Integer> statusCodes = new java.util.concurrent.CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ResponseEntity<Map> response = restTemplate.exchange(
                            url("/patients/{id}/names/{index}"), HttpMethod.DELETE, null, Map.class, id, 0);
                    statusCodes.add(response.getStatusCode().value());
                } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
                    statusCodes.add(e.getStatusCode().value());
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(statusCodes).hasSize(threadCount);
        assertThat(statusCodes).allSatisfy(code -> assertThat(code).isIn(200, 404, 409));
        assertThat(statusCodes).contains(200);
    }

    @Test
    void deleteAndPatch_concurrent_finalStateIsConsistent() throws InterruptedException {
        ResponseEntity<Map> created = restTemplate.postForEntity(url("/patients"), buildPatientPayload(), Map.class);
        long id = ((Number) created.getBody().get("id")).longValue();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<Integer> statusCodes = new java.util.concurrent.CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                startLatch.await();
                restTemplate.delete(url("/patients/{id}"), id);
                statusCodes.add(200);
            } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
                statusCodes.add(e.getStatusCode().value());
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                ResponseEntity<Map> response = restTemplate.exchange(
                        url("/patients/{id}"), HttpMethod.PATCH,
                        new HttpEntity<>(Map.of("age", 99)), Map.class, id);
                statusCodes.add(response.getStatusCode().value());
            } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
                statusCodes.add(e.getStatusCode().value());
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // patch-then-delete: both 200, patient is gone
        // delete-then-patch: delete 200, patch 404
        // truly concurrent: patch may get 409 (optimistic lock after delete)
        assertThat(statusCodes).allSatisfy(code -> assertThat(code).isIn(200, 404, 409));
        var patient = patientRepository.findById(id);
        if (patient.isPresent()) {
            assertThat(patient.get().getAge()).isEqualTo(99);
        }
    }

    @Test
    void syncPatient_concurrentSameFhirId_createsOnlyOneRow() throws InterruptedException {
        stubFhirRead("fhir-concurrent", buildFhirPatient("fhir-concurrent"));

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    restTemplate.postForEntity(url("/patients/sync/fhir-concurrent"), null, Map.class);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        long count = patientRepository.findAll().stream()
                .filter(p -> "fhir-concurrent".equals(p.getFhirId()))
                .count();
        assertThat(count).isEqualTo(1);
    }
}
