#!/usr/bin/env python3
"""Print desired GitHub settings, or apply them explicitly after the gate is green."""
import argparse
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--apply", action="store_true")
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
settings = json.loads((root / ".github/repository-settings.json").read_text())
if not args.apply:
    print(json.dumps(settings, indent=2))
    raise SystemExit(0)

repo = "repos/MohdFarag/bawsala"
def api(method, resource, payload=None):
    command = ["gh", "api", "--method", method, resource]
    if payload is not None:
        command += ["--input", "-"]
    completed = subprocess.run(command, input=json.dumps(payload) if payload is not None else None,
                               text=True, capture_output=True)
    if completed.returncode:
        raise SystemExit(f"GitHub refused {method} {resource}: {completed.stderr.strip()}")
    return json.loads(completed.stdout) if completed.stdout else None

api("PATCH", repo, settings["repository"])
api("PUT", repo + "/topics", {"names": settings["topics"]})
api("PUT", repo + "/private-vulnerability-reporting")
rulesets = api("GET", repo + "/rulesets")
existing = next((r for r in rulesets if r["name"] == settings["ruleset"]["name"]), None)
api("PUT" if existing else "POST", repo + "/rulesets" + (f"/{existing['id']}" if existing else ""), settings["ruleset"])
print("Repository settings applied. Verify the active ruleset in GitHub before merging.")
