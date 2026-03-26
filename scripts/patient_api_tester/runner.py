from __future__ import annotations

import subprocess
import sys
import urllib.error
from pathlib import Path

from .config import parse_args
from .lifecycle import LifecycleManager
from .scenarios import HelloWorldScenario, PatientApiScenario


def run() -> int:
    config = parse_args()

    root = LifecycleManager.find_project_root(Path(__file__).resolve())
    compose_file = root / "Fhir Server" / "docker-compose.yml"
    if not compose_file.exists():
        print(f"Missing docker compose file: {compose_file}", file=sys.stderr)
        return 2

    lifecycle = LifecycleManager(root=root, compose_file=compose_file)
    hello_scenario = HelloWorldScenario(config=config)
    patient_scenario = PatientApiScenario(config=config)

    if run_gradle_tests():
        print("[setup] Gradle tests already passed")
    else:
        return 2

    try:
        lifecycle.setup(target_url=config.hello_url, startup_timeout=config.startup_timeout)
        hello_scenario.run()
        patient_scenario.run()
        print("All Patient API checks passed.")
        return 0
    except urllib.error.URLError as err:
        print(f"Network error: {err}. Is the Spring Boot app running on {config.base_url}?", file=sys.stderr)
        return 2
    except subprocess.CalledProcessError as err:
        print(f"Command failed: {err}", file=sys.stderr)
        return 2
    except TimeoutError as err:
        print(str(err), file=sys.stderr)
        return 2
    except AssertionError as err:
        print(f"Test failed: {err}", file=sys.stderr)
        return 1
    finally:
        lifecycle.teardown()


def run_gradle_tests() -> bool:
    root = LifecycleManager.find_project_root(Path(__file__).resolve())
    gradle = root / "gradlew"
    cmd = [str(gradle), "clean", "test"]
    print(f"[setup] Running {' '.join(cmd)}")
    result = subprocess.run(
        cmd,
        cwd=root,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    print(result.stdout)
    if result.returncode != 0:
        print("[setup] Gradle tests failed", file=sys.stderr)
        return False
    return True
