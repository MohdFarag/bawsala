#!/usr/bin/env python3
"""Change selection and fail-closed aggregation shared by PR and release CI."""

import json
import os
import subprocess
import sys


def select(paths, full=False):
    selected = dict.fromkeys(("android", "server", "web", "devices"), full)
    for path in paths:
        if path.startswith(".github/") or path in ("version.txt", "release-please-config.json"):
            return dict.fromkeys(selected, True)
        if path.startswith("android/"):
            selected["android"] = selected["devices"] = True
        if path.startswith("server/"):
            selected["server"] = selected["devices"] = True
        if path.startswith("web/"):
            selected["web"] = True
        if path in (".prettierrc.json", ".editorconfig", ".gitattributes"):
            selected["server"] = selected["web"] = True
    return selected


def validate(needs):
    if needs["changes"]["result"] != "success":
        raise ValueError("Change detection failed; validation cannot establish coverage")
    outputs = needs["changes"]["outputs"]
    for job, result in needs.items():
        if job == "changes":
            continue
        if job in outputs and outputs[job] not in ("true", "false"):
            raise ValueError(f"{job}: invalid project selection")
        required = job not in outputs or outputs[job] == "true"
        expected = "success" if required else "skipped"
        if result["result"] != expected:
            raise ValueError(f"{job}: expected {expected}, got {result['result']}")


def main():
    if sys.argv[1] == "gate":
        validate(json.loads(os.environ["NEEDS_JSON"]))
        print("All applicable checks passed; excluded projects were explicitly skipped.")
        return
    full = os.environ.get("FORCE_FULL") == "true"
    base = os.environ.get("BASE_SHA", "")
    try:
        if not base or set(base) == {"0"}:
            full = True
            paths = []
        else:
            paths = subprocess.check_output(
                ["git", "diff", "--name-only", "--no-renames", "-z", base, "HEAD"]
            ).decode().split("\0")
    except subprocess.CalledProcessError:
        full, paths = True, []
    selected = select(paths, full)
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        for project, enabled in selected.items():
            print(f"{project}={str(enabled).lower()}", file=output)
    print(json.dumps(selected))


if __name__ == "__main__":
    main()
