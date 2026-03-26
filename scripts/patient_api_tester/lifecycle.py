from __future__ import annotations

import subprocess
import time
from pathlib import Path

from .http_client import request_json


class LifecycleManager:
    def __init__(self, root: Path, compose_file: Path):
        self.root = root
        self.compose_file = compose_file
        self.compose_dir = compose_file.parent
        self.spring_proc: subprocess.Popen | None = None

    @staticmethod
    def find_project_root(start: Path) -> Path:
        for current in [start, *start.parents]:
            if (current / "gradlew").exists():
                return current
        raise FileNotFoundError("Could not locate project root containing gradlew")

    def run_cmd(self, cmd: list[str], cwd: Path):
        print(f"$ {' '.join(cmd)}")
        subprocess.run(cmd, cwd=cwd, check=True)

    def wait_for_url(self, url: str, timeout_seconds: int):
        deadline = time.time() + timeout_seconds
        last_error = None
        while time.time() < deadline:
            try:
                status, _ = request_json("GET", url, timeout=2)
                if status < 500:
                    return
            except Exception as err:  # noqa: BLE001
                last_error = err
            time.sleep(2)
        raise TimeoutError(f"Timed out waiting for {url}. Last error: {last_error}")

    def setup(self, target_url: str, startup_timeout: int):
        print("[setup] Stop and remove existing docker containers + volumes")
        self.run_cmd(["docker", "compose", "-f", str(self.compose_file), "down", "-v"], cwd=self.compose_dir)

        print("[setup] Start docker containers fresh")
        self.run_cmd(["docker", "compose", "-f", str(self.compose_file), "up", "-d"], cwd=self.compose_dir)

        print("[setup] Wait for Postgres container to be healthy")
        self.wait_for_service_health("hapi-fhir-postgres", startup_timeout)

        print("[setup] Start Spring Boot app")
        self.spring_proc = subprocess.Popen(
            [str(self.root / "gradlew"), "bootRun"],
            cwd=self.root,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            start_new_session=True,
        )

        print(f"[setup] Wait for app readiness at {target_url}")
        self.wait_for_url(target_url, startup_timeout)

    def wait_for_service_health(self, service: str, timeout_seconds: int):
        container_id = self._get_service_container_id(service)
        deadline = time.time() + timeout_seconds
        last_status: str | None = None
        while time.time() < deadline:
            status = self._inspect_health_status(container_id)
            if status == "healthy":
                return
            last_status = status
            time.sleep(2)
        raise TimeoutError(
            f"Timed out waiting for container {container_id} ({service}) to be healthy. Last status: {last_status}"
        )

    def _get_service_container_id(self, service: str) -> str:
        args = ["docker", "compose", "-f", str(self.compose_file), "ps", "-q", service]
        result = subprocess.run(args, cwd=self.compose_dir, capture_output=True, text=True)
        if result.returncode != 0:
            raise subprocess.CalledProcessError(result.returncode, args, output=result.stdout, stderr=result.stderr)
        container_id = result.stdout.strip().splitlines()[0] if result.stdout.strip() else ""
        if not container_id:
            raise RuntimeError(f"Could not resolve container id for service '{service}'")
        return container_id

    def _inspect_health_status(self, container_id: str) -> str:
        args = ["docker", "inspect", "-f", "{{.State.Health.Status}}", container_id]
        result = subprocess.run(args, capture_output=True, text=True)
        if result.returncode != 0:
            raise subprocess.CalledProcessError(result.returncode, args, output=result.stdout, stderr=result.stderr)
        return result.stdout.strip() or "unknown"

    def teardown(self):
        print("[teardown] Stop Spring Boot app")
        self.stop_process(self.spring_proc)

        print("[teardown] Stop and remove docker containers + volumes")
        try:
            self.run_cmd(["docker", "compose", "-f", str(self.compose_file), "down", "-v"], cwd=self.compose_dir)
        except subprocess.CalledProcessError:
            print("[teardown] Docker cleanup failed")

    @staticmethod
    def stop_process(proc: subprocess.Popen | None):
        if proc is None or proc.poll() is not None:
            return

        proc.terminate()
        try:
            proc.wait(timeout=15)
            return
        except subprocess.TimeoutExpired:
            pass

        proc.kill()
        proc.wait(timeout=5)
