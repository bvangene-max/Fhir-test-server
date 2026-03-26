# Patient API

Base URL: `http://localhost:8081`

All request and response bodies are JSON. All endpoints return the full patient object on success unless stated otherwise.

---

## Patient object

```json
{
  "id": 1,
  "version": 0,
  "fhirId": "abc123",
  "name": "Jane Doe",
  "age": 34,
  "names": [
    { "family": "Doe", "given": "Jane", "use": "official" }
  ],
  "addresses": [
    { "line": "123 Main St", "city": "Brussels", "postalCode": "1000", "country": "BE", "use": "home" }
  ],
  "phoneNumbers": [
    { "system": "phone", "value": "+321234567", "use": "mobile" }
  ]
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | number | Auto-generated. Must not be set on create. |
| `version` | number | Managed internally for optimistic locking. Do not set manually. |
| `fhirId` | string | Optional. Must be unique if provided. |
| `name` | string | Required. Must not be blank. |
| `age` | integer | Required. Must be between 0 and 150 inclusive. |
| `names` | array | Structured name entries (see below). |
| `addresses` | array | Structured address entries (see below). |
| `phoneNumbers` | array | Structured phone/contact entries (see below). |

### Name object
| Field | Type | Notes |
|---|---|---|
| `family` | string | Family (last) name. |
| `given` | string | Given (first) name(s), space-separated if multiple. |
| `use` | string | e.g. `official`, `nickname`, `usual` |

### Address object
| Field | Type | Notes |
|---|---|---|
| `line` | string | Street address. |
| `city` | string | |
| `postalCode` | string | |
| `country` | string | |
| `use` | string | e.g. `home`, `work`, `temp` |

### Phone number object
| Field | Type | Notes |
|---|---|---|
| `system` | string | e.g. `phone`, `email`, `fax` |
| `value` | string | The actual number or address. |
| `use` | string | e.g. `mobile`, `home`, `work` |

---

## Endpoints

### GET /patients
Returns all patients.

**Response:** `200 OK` — array of patient objects (empty array if none exist).

```bash
curl http://localhost:8081/patients
```

```json
[
  {
    "id": 1,
    "version": 0,
    "fhirId": null,
    "name": "Jane Doe",
    "age": 34,
    "names": [{ "family": "Doe", "given": "Jane", "use": "official" }],
    "addresses": [{ "line": "123 Main St", "city": "Brussels", "postalCode": "1000", "country": "BE", "use": "home" }],
    "phoneNumbers": []
  }
]
```

---

### GET /patients/{id}
Returns a single patient by internal ID.

**Response:** `200 OK` — patient object.

| Status | Reason |
|---|---|
| `404 Not Found` | No patient with that ID. |

```bash
curl http://localhost:8081/patients/1
```

---

### POST /patients
Creates a new patient.

**Rules:**
- `name` and `age` are required.
- `id` must not be set in the request body.
- `age` must be between 0 and 150.
- `fhirId` must be unique if provided.

**Response:** `201 Created` — created patient object with generated `id`.

| Status | Reason |
|---|---|
| `400 Bad Request` | `name` is missing/blank, `age` is missing/out of range, `id` was set, or a field has the wrong type. |
| `409 Conflict` | A patient with that `fhirId` already exists. |

```bash
curl -X POST http://localhost:8081/patients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "age": 34,
    "names": [{ "family": "Doe", "given": "Jane", "use": "official" }],
    "addresses": [{ "line": "123 Main St", "city": "Brussels", "postalCode": "1000", "country": "BE", "use": "home" }],
    "phoneNumbers": [{ "system": "phone", "value": "+321234567", "use": "mobile" }]
  }'
```

```json
{
  "id": 1,
  "version": 0,
  "fhirId": null,
  "name": "Jane Doe",
  "age": 34,
  "names": [{ "family": "Doe", "given": "Jane", "use": "official" }],
  "addresses": [{ "line": "123 Main St", "city": "Brussels", "postalCode": "1000", "country": "BE", "use": "home" }],
  "phoneNumbers": [{ "system": "phone", "value": "+321234567", "use": "mobile" }]
}
```

---

### PATCH /patients/{id}
Partially updates a patient. Only the fields included in the body are changed. Omitted fields keep their current values.

Sending an empty array for `names`, `addresses`, or `phoneNumbers` clears that list entirely.

**Response:** `200 OK` — updated patient object.

| Status | Reason |
|---|---|
| `400 Bad Request` | Invalid field type or `age` out of range. |
| `404 Not Found` | No patient with that ID. |
| `409 Conflict` | Another request modified the patient concurrently — retry. |

```bash
# Update only the name
curl -X PATCH http://localhost:8081/patients/1 \
  -H "Content-Type: application/json" \
  -d '{ "name": "Jane Smith", "age": 35 }'
```

```bash
# Clear all addresses
curl -X PATCH http://localhost:8081/patients/1 \
  -H "Content-Type: application/json" \
  -d '{ "addresses": [] }'
```

```bash
# Replace the names list entirely
curl -X PATCH http://localhost:8081/patients/1 \
  -H "Content-Type: application/json" \
  -d '{
    "names": [{ "family": "Smith", "given": "Jane", "use": "official" }]
  }'
```

---

### DELETE /patients/{id}
Deletes a patient. Silently succeeds if the patient does not exist.

**Response:** `200 OK` — no body.

| Status | Reason |
|---|---|
| `409 Conflict` | Another request modified the patient concurrently — retry. |

```bash
curl -X DELETE http://localhost:8081/patients/1
```

---

### POST /patients/sync/{fhirId}
Fetches a patient from the external HAPI FHIR R4 server (expected at `http://localhost:8080/fhir`) and stores it locally. If a patient with that `fhirId` already exists locally it is updated in place; otherwise a new record is created.

**Response:** `200 OK` — the stored patient object.

| Status | Reason |
|---|---|
| `400 Bad Request` | FHIR patient has an invalid birth date (e.g. in the future). |
| `404 Not Found` | FHIR server has no patient with that ID. |
| `502 Bad Gateway` | FHIR server is unreachable or returned an error. |

```bash
curl -X POST http://localhost:8081/patients/sync/abc123
```

```json
{
  "id": 2,
  "version": 0,
  "fhirId": "abc123",
  "name": "John Smith",
  "age": 35,
  "names": [{ "family": "Smith", "given": "John", "use": "official" }],
  "addresses": [],
  "phoneNumbers": []
}
```

---

### POST /patients/{id}/names
Appends a name entry to the patient's names list.

**Response:** `200 OK` — updated patient object.

| Status | Reason |
|---|---|
| `404 Not Found` | No patient with that ID. |
| `409 Conflict` | Another request modified the patient concurrently — retry. |

```bash
curl -X POST http://localhost:8081/patients/1/names \
  -H "Content-Type: application/json" \
  -d '{ "family": "Doe", "given": "Jane", "use": "official" }'
```

---

### DELETE /patients/{id}/names/{index}
Removes the name at the given zero-based index from the patient's names list.

**Response:** `200 OK` — updated patient object.

| Status | Reason |
|---|---|
| `404 Not Found` | No patient with that ID, or index is out of bounds. |
| `409 Conflict` | Another request modified the patient concurrently — retry. |

```bash
# Remove the first name entry
curl -X DELETE http://localhost:8081/patients/1/names/0
```

---

### POST /patients/{id}/addresses
Appends an address entry to the patient's addresses list.

**Response:** `200 OK` — updated patient object.

| Status | Reason |
|---|---|
| `404 Not Found` | No patient with that ID. |
| `409 Conflict` | Another request modified the patient concurrently — retry. |

```bash
curl -X POST http://localhost:8081/patients/1/addresses \
  -H "Content-Type: application/json" \
  -d '{ "line": "456 New St", "city": "Ghent", "postalCode": "9000", "country": "BE", "use": "work" }'
```

---

### DELETE /patients/{id}/addresses/{index}
Removes the address at the given zero-based index from the patient's addresses list.

**Response:** `200 OK` — updated patient object.

| Status | Reason |
|---|---|
| `404 Not Found` | No patient with that ID, or index is out of bounds. |
| `409 Conflict` | Another request modified the patient concurrently — retry. |

```bash
# Remove the first address entry
curl -X DELETE http://localhost:8081/patients/1/addresses/0
```

---

### POST /patients/{id}/phone-numbers
Appends a phone number entry to the patient's phone numbers list.

**Response:** `200 OK` — updated patient object.

| Status | Reason |
|---|---|
| `404 Not Found` | No patient with that ID. |
| `409 Conflict` | Another request modified the patient concurrently — retry. |

```bash
curl -X POST http://localhost:8081/patients/1/phone-numbers \
  -H "Content-Type: application/json" \
  -d '{ "system": "phone", "value": "+321234567", "use": "mobile" }'
```

---

### DELETE /patients/{id}/phone-numbers/{index}
Removes the phone number at the given zero-based index from the patient's phone numbers list.

**Response:** `200 OK` — updated patient object.

| Status | Reason |
|---|---|
| `404 Not Found` | No patient with that ID, or index is out of bounds. |
| `409 Conflict` | Another request modified the patient concurrently — retry. |

```bash
# Remove the first phone number entry
curl -X DELETE http://localhost:8081/patients/1/phone-numbers/0
```

---

## Concurrency

All write endpoints use optimistic locking. If two requests modify the same patient simultaneously, one will succeed with `200` and the other will receive `409 Conflict`. The client is expected to retry on 409.

The `version` field in the response reflects the current lock version and increments on every successful write.
