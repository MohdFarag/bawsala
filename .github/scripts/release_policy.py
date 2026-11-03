"""Never move stable release aliases backwards when retrying an older tag."""
import re
import sys


def version(tag):
    match = re.fullmatch(r"v(\d+)\.(\d+)\.(\d+)", tag)
    return tuple(map(int, match.groups())) if match else None


def promotions(tag, published):
    target = version(tag)
    if target is None:
        raise ValueError("A stable vX.Y.Z tag is required")
    versions = [v for name in published if (v := version(name)) is not None] + [target]
    return {
        "latest": target == max(versions),
        "series": target == max(v for v in versions if v[:2] == target[:2]),
    }


if __name__ == "__main__":
    for key, enabled in promotions(sys.argv[1], open(sys.argv[2]).read().splitlines()).items():
        print(f"{key}={str(enabled).lower()}")
