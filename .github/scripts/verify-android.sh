#!/usr/bin/env bash
# Verify final artifacts, not merely the keystore supplied to the build.
set -euo pipefail
directory=${1:?artifact directory}
expected=${ANDROID_SIGNING_CERT_SHA256:?Set the fingerprint from the previous official APK}
expected=$(tr -d ':' <<< "$expected" | tr '[:upper:]' '[:lower:]' | tr -d '\n')
[[ "$expected" =~ ^[0-9a-f]{64}$ ]] || { echo 'Invalid signing certificate fingerprint' >&2; exit 1; }
tools="${ANDROID_HOME:?}/build-tools/35.0.0"
apk="$directory/app-release.apk"
aab="$directory/app-release.aab"
"$tools/apksigner" verify --verbose --print-certs "$apk" > "$directory/apk-signature.txt"
actual=$(awk '/Signer #1 certificate SHA-256 digest:/ {print $NF}' "$directory/apk-signature.txt")
[[ "$actual" == "$expected" ]] || { echo 'APK signing certificate changed' >&2; exit 1; }
jarsigner -verify "$aab" > "$directory/aab-signature.txt"
grep -q 'jar verified.' "$directory/aab-signature.txt"
keytool -J-Duser.language=en -printcert -jarfile "$aab" > "$directory/aab-certificate.txt"
bundle=$(awk '/SHA256:/ {print $2; exit}' "$directory/aab-certificate.txt" | tr -d ':' | tr '[:upper:]' '[:lower:]')
[[ "$bundle" == "$expected" ]] || { echo 'AAB signing certificate changed' >&2; exit 1; }
"$tools/aapt" dump badging "$apk" > "$directory/apk-metadata.txt"
python3 - "$directory/apk-metadata.txt" <<'PY'
import os, re, sys
metadata = open(sys.argv[1]).read()
assert "name='app.bawsala'" in metadata, 'Unexpected application ID'
assert re.search(r"versionName='([^']+)'", metadata)[1] == os.environ['VERSION_NAME'], 'Version name mismatch'
assert int(re.search(r"versionCode='(\d+)'", metadata)[1]) > 0, 'Invalid version code'
PY
(cd "$directory" && sha256sum --check SHA256SUMS.txt)
