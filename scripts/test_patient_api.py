#!/usr/bin/env python3
"""
Modular integration test entrypoint for PatientController endpoints.
"""

from __future__ import annotations

import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from patient_api_tester.runner import run  # noqa: E402


if __name__ == "__main__":
    raise SystemExit(run())
