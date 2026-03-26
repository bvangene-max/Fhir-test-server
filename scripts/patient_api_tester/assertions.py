from __future__ import annotations


def assert_true(condition: bool, message: str):
    if not condition:
        raise AssertionError(message)
