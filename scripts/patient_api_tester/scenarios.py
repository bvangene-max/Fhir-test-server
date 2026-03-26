from __future__ import annotations

import time

from .assertions import assert_true
from .config import TestConfig
from .http_client import request_json


class HelloWorldScenario:
    EXPECTED_GREETING = "hello world"

    def __init__(self, config: TestConfig):
        self.config = config

    def run(self):
        print("[TEST] GET /hello (temporary check)")
        status, body = request_json("GET", self.config.hello_url, timeout=self.config.timeout)
        assert_true(status == 200, f"Expected 200, got {status}")
        assert_true(isinstance(body, str), f"Expected text response, got {type(body)}")
        assert_true(
            body.strip() == self.EXPECTED_GREETING,
            f"Expected '{self.EXPECTED_GREETING}', got '{body}'",
        )


class PatientApiScenario:
    SAMPLE_PATIENT = {
        "name": "Integration Tester",
        "age": 37,
        "names": [
            {
                "family": "Tester",
                "given": "Integration",
                "use": "official",
            }
        ],
        "addresses": [
            {
                "line": "123 Integration Way",
                "city": "Brussels",
                "postalCode": "1000",
                "country": "BE",
                "use": "home",
            }
        ],
        "phoneNumbers": [
            {
                "system": "phone",
                "value": "+321234567",
                "use": "mobile",
            }
        ],
    }
    FHIR_SYNC_PAYLOAD = {
        "resourceType": "Patient",
        "name": [
            {
                "use": "official",
                "family": "Sync",
                "given": ["FHIR"],
            }
        ],
        "telecom": [
            {
                "system": "phone",
                "value": "+321000000",
                "use": "mobile",
            }
        ],
        "address": [
            {
                "line": ["123 Sync Ave"],
                "city": "Brussels",
                "postalCode": "1000",
                "country": "BE",
                "use": "home",
            }
        ],
        "birthDate": "1990-01-01",
    }
    EXPECTED_SYNC_NAMES = [
        {
            "family": "Sync",
            "given": "FHIR",
            "use": "official",
        }
    ]
    EXPECTED_SYNC_ADDRESSES = [
        {
            "line": "123 Sync Ave",
            "city": "Brussels",
            "postalCode": "1000",
            "country": "BE",
            "use": "home",
        }
    ]
    EXPECTED_SYNC_PHONES = FHIR_SYNC_PAYLOAD["telecom"]

    def __init__(self, config: TestConfig):
        self.config = config
        self.created_id: int | str | None = None

    def run(self):
        self.create_patient()
        self.retrieve_patient()
        self.delete_patient()
        self.verify_patient_removed()
        self.sync_patient_from_fhir()

    def create_patient(self):
        print("[TEST] POST /patients (create)")
        status, body = request_json("POST", self.config.patients_url, self.SAMPLE_PATIENT, timeout=self.config.timeout)
        assert_true(status in (200, 201), f"Expected 200/201, got {status}")
        assert_true(isinstance(body, dict), f"Expected object response, got {type(body)}")
        self.created_id = body.get("id")
        assert_true(self.created_id is not None, f"Created patient did not return id: {body}")
        self._assert_patient_data(body, self.SAMPLE_PATIENT)

    def retrieve_patient(self):
        print("[TEST] GET /patients/{id}")
        status, body = request_json("GET", self._patient_url(), timeout=self.config.timeout)
        assert_true(status == 200, f"Expected 200, got {status}")
        assert_true(isinstance(body, dict), f"Expected object response, got {type(body)}")
        self._assert_patient_data(body, self.SAMPLE_PATIENT)

    def delete_patient(self):
        print("[TEST] DELETE /patients/{id}")
        status, _ = request_json("DELETE", self._patient_url(), timeout=self.config.timeout)
        assert_true(status in (200, 202, 204), f"Expected 2xx, got {status}")

    def verify_patient_removed(self):
        print("[TEST] GET /patients/{id} after delete (expect missing)")
        status, body = request_json("GET", self._patient_url(), timeout=self.config.timeout)
        assert_true(status in (200, 404), f"Expected 200/404 after delete, got {status}")
        assert_true(body is None, f"Expected no body after delete, got {body}")

    def sync_patient_from_fhir(self):
        print("[TEST] POST /fhir/Patient (seed for sync)")
        self._wait_for_fhir_server()
        status, body = request_json("POST", f"{self.config.fhir_url}/Patient", self.FHIR_SYNC_PAYLOAD)
        assert_true(status in (200, 201), f"Expected 200/201 from FHIR create, got {status}")
        fhir_id = body.get("id") if isinstance(body, dict) else None
        assert_true(fhir_id is not None, f"FHIR create response missing id: {body}")

        app_patient_id: int | str | None = None
        try:
            print("[TEST] POST /patients/sync/{id}")
            status, saved = request_json("POST", f"{self.config.patients_url}/sync/{fhir_id}", timeout=self.config.timeout)
            assert_true(status in (200, 201), f"Expected 200/201 from sync, got {status}")
            assert_true(isinstance(saved, dict), f"Expected dict response from sync, got {type(saved)}")
            app_patient_id = saved.get("id")
            assert_true(app_patient_id is not None, f"Synced patient missing id: {saved}")
            self._assert_synced_patient(saved, fhir_id)

            print("[TEST] GET /patients/{id} (after sync)")
            status, retrieved = request_json("GET", f"{self.config.patients_url}/{app_patient_id}", timeout=self.config.timeout)
            assert_true(status == 200, f"Expected 200 when fetching synced patient, got {status}")
            assert_true(isinstance(retrieved, dict), f"Expected dict, got {type(retrieved)}")
            self._assert_synced_patient(retrieved, fhir_id)
        finally:
            request_json("DELETE", f"{self.config.fhir_url}/Patient/{fhir_id}")
            if app_patient_id:
                request_json("DELETE", f"{self.config.patients_url}/{app_patient_id}")

    @staticmethod
    def _assert_synced_patient(body: dict[str, object], fhir_id: str):
        assert_true(body.get("fhirId") == fhir_id, f"Expected fhirId={fhir_id}, got {body.get('fhirId')}")
        assert_true(body.get("names") == PatientApiScenario.EXPECTED_SYNC_NAMES,
                    f"Expected names={PatientApiScenario.EXPECTED_SYNC_NAMES}, got {body.get('names')}")
        assert_true(body.get("addresses") == PatientApiScenario.EXPECTED_SYNC_ADDRESSES,
                    f"Expected addresses={PatientApiScenario.EXPECTED_SYNC_ADDRESSES}, got {body.get('addresses')}")
        assert_true(body.get("phoneNumbers") == PatientApiScenario.EXPECTED_SYNC_PHONES,
                    f"Expected phoneNumbers={PatientApiScenario.EXPECTED_SYNC_PHONES}, got {body.get('phoneNumbers')}")

    def _wait_for_fhir_server(self, timeout: int = 60):
        deadline = time.time() + timeout
        last_error = None
        while time.time() < deadline:
            try:
                status, _ = request_json("GET", f"{self.config.fhir_url}/metadata", timeout=2)
                if status == 200:
                    return
                last_error = f"Metadata returned {status}"
            except Exception as err:  # noqa: BLE001
                last_error = err
            time.sleep(2)
        raise TimeoutError(f"FHIR server at {self.config.fhir_url} did not become healthy. Last error: {last_error}")

    def _patient_url(self) -> str:
        assert_true(self.created_id is not None, "No patient id available")
        return f"{self.config.patients_url}/{self.created_id}"

    @staticmethod
    def _assert_patient_data(body: dict[str, object], expected: dict[str, object]):
        for key in ("name", "age"):
            assert_true(
                body.get(key) == expected.get(key),
                f"Expected {key}={expected.get(key)}, got {body.get(key)}",
            )

        for key in ("names", "addresses", "phoneNumbers"):
            expected_list = expected.get(key, [])
            actual_list = body.get(key, [])
            assert_true(
                isinstance(actual_list, list),
                f"Expected {key} in response to be a list, got {type(actual_list)}",
            )
            assert_true(
                actual_list == expected_list,
                f"Expected {key}={expected_list}, got {actual_list}",
            )
