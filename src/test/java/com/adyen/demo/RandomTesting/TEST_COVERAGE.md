# Test Coverage Map

This file is a quick map of what the current test suite covers and what still looks untested.

## Controller Integration Tests

| Test class | What it covers |
| --- | --- |
| `PatientCrudIT` | Create, read, list, and delete patient endpoints; generated ids; empty list; 404 on missing patient; nested `names` and `addresses` in responses. |
| `PatientPatchIT` | PATCH updates, ignored body `id`, list replacement, empty list clearing, and 404 on missing patient. |
| `PatientSubCollectionIT` | Add/remove endpoints for names, addresses, and phone numbers; 404 handling; index-based deletes; malformed JSON rejection; and unknown-field rejection for sub-collection payloads. |
| `PatientValidationIT` | Age validation boundaries, invalid age types, unknown-field rejection at top level and in nested objects, invalid nested field types, malformed JSON, empty request bodies, and strict input handling for POST and PATCH. |
| `PatientVersionIT` | `version` field exposure and increment behavior after updates and sub-collection changes. |
| `PatientSyncIT` | Sync endpoint happy path, idempotency, update behavior, FHIR error mapping, future birth date handling, and nested collection mapping. |
| `PatientConcurrencyIT` | Concurrent create/update/delete/sync behavior, conflict handling, optimistic locking, and final-state consistency. |

## Service Unit Tests

| Test class | What it covers |
| --- | --- |
| `PatientCrudServiceTest` | Repository delegation for get/save/delete and empty/not-found cases. |
| `PatientSubCollectionServiceTest` | Adding and removing names, including not-found behavior. |
| `PatientSyncServiceTest` | Sync flow for existing/new patients, duplicate insert handling, and FHIR exception propagation. |

## Mapper Unit Tests

| Test class | What it covers |
| --- | --- |
| `PatientMapperAgeTest` | Birth date to age conversion, missing birth date, and future birth date rejection. |
| `PatientMapperScalarTest` | FHIR id mapping, name mapping, age calculation, missing names, multi-given names, and null name use. |
| `PatientMapperSubCollectionTest` | Address mapping, telecom mapping, multiple collections, and update-in-place behavior. |

## Lightweight Smoke Test

| Test class | What it covers |
| --- | --- |
| `DemoApplicationTests` | Application context loads. |

## Current Gaps / Likely Missing Coverage

- The sync endpoint does not currently have request-shape validation cases because its input is path-based, but its response shape coverage is stronger than its error-body coverage.

## Notes

- This map is intentionally descriptive, not a strict completeness guarantee.
- If a new endpoint or behavior is added, the matching test class should be updated in this file at the same time.
