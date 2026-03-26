# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew clean build
./gradlew clean build -x test   # skip tests

# Run
./gradlew bootRun               # app runs on port 8081

# Test
./gradlew test
./gradlew test --tests "com.adyen.demo.RandomTesting.service.PatientCrudServiceTest"  # single test class

# Full stack (Spring Boot app + PostgreSQL)
docker compose -f "Fhir Server/docker-compose.yml" up -d
docker compose -f "Fhir Server/docker-compose.yml" down -v

# Integration tests (Python)
python3 ./scripts/test_patient_api.py [--base-url http://localhost:8081] [--fhir-url http://localhost:8080/fhir]

# DB inspection
./scripts/check_postgres.sh table   # show patient table schema
./scripts/check_postgres.sh shell   # interactive psql
```

## Architecture

Spring Boot (port 8081) REST API that manages Patient resources locally and can sync from an external HAPI FHIR R4 server (expected at port 8080). Persists to PostgreSQL via JPA.

**Key packages** under `com.adyen.demo.RandomTesting`:
- `controller/` — REST layer. `PatientController` exposes `/patients` CRUD endpoints plus `POST /patients/sync/{fhirId}` to pull a patient from the external FHIR server.
- `service/` — Business logic. `PatientService` orchestrates repository calls and FHIR fetches. `PatientMapper` converts `org.hl7.fhir.r4.model.Patient` (HAPI) into the internal JPA `InternalPatient` entity.
- `entity/` — JPA `InternalPatient` with `@ElementCollection` sub-entities: `Name`, `PatientAddress`, `PhoneNumber`.
- `dto/` — `PatientRequest` is the inbound DTO for POST. Validated with `@NotBlank` (name), `@NotNull @Min(0) @Max(150)` (age). Unknown fields are rejected.
- `repository/` — Spring Data JPA `PatientRepository`.
- `FhirConfig/` — Spring `@Configuration` that provides `FhirContext` (R4) and `IGenericClient` beans.

**Sync flow:** `POST /patients/sync/{fhirId}` → `PatientService` fetches from external FHIR server via `IGenericClient` → `PatientMapper` converts → `PatientRepository.save()`.

**Sub-collection endpoints:** Names, addresses, and phone numbers each have their own `POST /{id}/names` and `DELETE /{id}/names/{index}` endpoints. Removal is index-based. `@Retryable` on service methods handles `OptimisticLockingFailureException` (up to 5 attempts); the controller also retries PATCH up to 5 times before returning 409.

## HTTP status codes

- `POST /patients` → 201 Created
- `GET`, `PATCH`, sub-collection mutations → 200 OK
- Missing/invalid patient → 404
- Validation failure → 400
- Concurrent edit exhausted retries → 409
- FHIR server unreachable → 502

## Test structure

Tests use H2 in-memory DB. `IGenericClient` is mocked with `@MockitoBean`.

- `service/` — Unit tests with Mockito mocks. Base class: `PatientServiceTestBase`.
- `controller/` — Full-stack integration tests (`@SpringBootTest(RANDOM_PORT)`). Base class: `PatientControllerITBase` provides `restTemplate`, `buildPatientPayload()`, `expectClientError(status, lambda)`, `createPatient()`, `patchPatient()`, `deleteFromCollection()`.

## Tech Stack

- Java 21, Spring Boot 4.0.3, Gradle 9.3.1
- HAPI FHIR 6.10.0 (`hapi-fhir-base`, `hapi-fhir-client`, `hapi-fhir-structures-r4`)
- Spring Data JPA + Hibernate, PostgreSQL 16
- Lombok
- H2 in-memory DB for unit/integration tests

## Configuration

`src/main/resources/application.properties` — app port (8081), datasource (`jdbc:postgresql://localhost:5432/hapi`, user/pass: `admin`/`admin`), `ddl-auto=update`.

The external FHIR server URL is configured in `FhirConfig.java` (hardcoded to `http://localhost:8080/fhir`).
