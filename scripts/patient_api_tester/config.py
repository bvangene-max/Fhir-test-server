from __future__ import annotations

import argparse
from dataclasses import dataclass


@dataclass
class TestConfig:
    base_url: str
    fhir_url: str
    timeout: int
    startup_timeout: int

    @property
    def hello_url(self) -> str:
        return f"{self.base_url.rstrip('/')}/hello"

    @property
    def patients_url(self) -> str:
        return f"{self.base_url.rstrip('/')}/patients"



def parse_args() -> TestConfig:
    parser = argparse.ArgumentParser(description="Test Patient API endpoints")
    parser.add_argument("--base-url", default="http://localhost:8081", help="API base URL")
    parser.add_argument("--fhir-url", default="http://localhost:8080/fhir", help="FHIR server base URL")
    parser.add_argument("--timeout", type=int, default=5, help="HTTP timeout in seconds")
    parser.add_argument("--startup-timeout", type=int, default=180, help="Startup wait timeout in seconds")
    args = parser.parse_args()
    return TestConfig(
        base_url=args.base_url,
        fhir_url=args.fhir_url,
        timeout=args.timeout,
        startup_timeout=args.startup_timeout,
    )
