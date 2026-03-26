from __future__ import annotations

import json
import urllib.error
import urllib.request
from typing import Any, Union


JsonBody = Union[dict[str, Any], list[Any], str, None]


def _parse_body(raw: str) -> JsonBody:
    if not raw:
        return None
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return raw


def request_json(method: str, url: str, payload: dict[str, Any] | None = None, timeout: int = 5):
    data = None
    headers = {}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            raw = response.read().decode("utf-8")
            return response.status, _parse_body(raw)
    except urllib.error.HTTPError as err:
        raw = err.read().decode("utf-8", errors="replace")
        return err.code, _parse_body(raw)
