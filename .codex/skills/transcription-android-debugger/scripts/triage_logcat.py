#!/usr/bin/env python3
"""Extract high-signal Android failure lines from a Logcat export."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


DEFAULT_PACKAGE = "com.transcriptionmodel.ideacapture"
SIGNALS = (
    ("fatal", re.compile(r"FATAL EXCEPTION", re.I)),
    ("android-runtime", re.compile(r"\bE\s+AndroidRuntime\b|\bAndroidRuntime\b.*\bE\b", re.I)),
    ("cause", re.compile(r"\bCaused by:\s|\b[\w$.]*(?:Exception|Error):\s", re.I)),
    ("anr", re.compile(r"\bANR in\b|Input dispatching timed out", re.I)),
    ("build", re.compile(r"(?:^|\s)e:\s+.*\.kt:\d+|Execution failed for task|FAILURE: Build failed", re.I)),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Show crash, ANR, build, package, and app-frame signals from Logcat."
    )
    parser.add_argument("logcat", nargs="?", help="Logcat file; omit or use - for stdin")
    parser.add_argument("--package", default=DEFAULT_PACKAGE, help="Application package")
    parser.add_argument("--context", type=int, default=2, help="Lines before and after each signal")
    parser.add_argument("--max-blocks", type=int, default=40, help="Maximum signal blocks to print")
    return parser.parse_args()


def read_lines(path: str | None) -> list[str]:
    if path in (None, "-"):
        return sys.stdin.read().splitlines()
    return Path(path).read_text(encoding="utf-8", errors="replace").splitlines()


def classify(line: str, package: str) -> list[str]:
    labels = [label for label, pattern in SIGNALS if pattern.search(line)]
    if re.search(r"\bProcess:\s*" + re.escape(package) + r"(?:,|\b)", line):
        labels.append("package")
    if re.search(r"\bat\s+" + re.escape(package) + r"(?:\.|\b)", line):
        labels.append("app-frame")
    return labels


def main() -> int:
    args = parse_args()
    if args.context < 0 or args.max_blocks < 1:
        raise SystemExit("--context must be >= 0 and --max-blocks must be >= 1")

    try:
        lines = read_lines(args.logcat)
    except OSError as exc:
        print(f"Could not read Logcat: {exc}", file=sys.stderr)
        return 2

    hits: list[tuple[int, list[str]]] = []
    for index, line in enumerate(lines):
        labels = classify(line, args.package)
        if labels:
            hits.append((index, labels))

    if not hits:
        print(f"No high-signal lines found for {args.package}.")
        return 1

    grouped: list[dict[str, object]] = []
    for index, labels in hits:
        start = max(0, index - args.context)
        end = min(len(lines), index + args.context + 1)
        if grouped and start <= int(grouped[-1]["end"]):
            grouped[-1]["end"] = max(int(grouped[-1]["end"]), end)
            grouped[-1]["hits"].append((index, labels))  # type: ignore[union-attr]
        else:
            grouped.append({"start": start, "end": end, "hits": [(index, labels)]})

    for block_number, block in enumerate(grouped[: args.max_blocks], start=1):
        block_hits = block["hits"]
        hit_indexes = {index for index, _ in block_hits}  # type: ignore[union-attr]
        block_labels = list(
            dict.fromkeys(label for _, labels in block_hits for label in labels)  # type: ignore[union-attr]
        )
        print(f"\n--- signal block {block_number}: {', '.join(block_labels)} ---")
        for line_index in range(int(block["start"]), int(block["end"])):
            marker = ">" if line_index in hit_indexes else " "
            print(f"{marker}{line_index + 1:7d} | {lines[line_index]}")
        if block_number >= args.max_blocks:
            break

    print(
        "\nConfirm the first meaningful failure in the surrounding raw log; "
        "later exceptions may be downstream."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
