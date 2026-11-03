# Repository hardening

The audit fixes are delivered as focused, dependent PRs. Merge in order: #81 (CI), #82 (releases),
#83 (visual regression), #84 (device tests), #85 (security), then #92 (documentation).
The owner merges; updating a stacked branch means merging the latest base and reviewing conflicts,
never force-pushing or automatically preferring one side.

## Owner rollout checklist

- [x] Observe a green **Repository quality gate** on #81. It is emitted even for documentation-only PRs.
- [ ] Apply `.github/repository-settings.json` using
  `python3 .github/scripts/configure-repository.py --apply`, or use its values in GitHub Settings.
  The default command prints the settings without changing GitHub. It needs administration permission;
  ordinary repository read/write access may be insufficient. Do not temporarily disable protection.
- [ ] Verify required checks, strict branch freshness, conversation resolution, squash-only merges,
  linear history, force-push/deletion blocks, and zero required external approvals.
- [x] Confirm private vulnerability reporting is enabled and the Security tab receives CodeQL results.
- [ ] Review the 44 initial screenshot baselines against Penpot references. Subsequent changes must
  carry their baseline changes explicitly; never record during normal CI to hide a regression.
- [ ] Exercise one fork PR: validation must pass without secrets or write-only annotation steps.
- [x] Confirm the API 35 smoke suite and API 28/35 extended suites pass on GitHub-hosted runners.
- [ ] In the `release` environment, retain the existing four signing secrets and configure
  `ANDROID_SIGNING_CERT_SHA256` from a previously trusted official APK. Do not derive trust from the
  new key being tested. With Android SDK build tools: `apksigner verify --print-certs app-release.apk`.
- [ ] Verify `API_SERVER_URL` names the intended production server. Never store signing keys in Git.
- [ ] Merge a release PR only after the fixes above land. Confirm draft → verified packages/image →
  published release, APK/AAB signatures, two checksum entries, and image provenance/SBOM.
- [ ] Install the new signed APK over the previous official release on a device; verify existing
  captures and account data remain. Missing original signing keys block this step and publication.

The current private-reporting endpoint was confirmed enabled during implementation.
The [#84 acceptance run](https://github.com/MohdFarag/bawsala/actions/runs/41610421295) passed
the API 28 and API 35 extended suites. The [#85 CI run](https://github.com/MohdFarag/bawsala/actions/runs/41621599310)
passed the API 35 smoke suite, both CodeQL scans and their Security-tab uploads, and the quality gate.
The trusted v0.10.0 APK was downloaded and verified with Android `apksigner`; its certificate is
`CN=Bawsala, O=MohdFarag, C=EG`, SHA-256:
`1b046bd9e752c5d0044ebb99e86733039b7f6662a5f5870d2569ba6cf3deac3c`.
Configure that fingerprint only after checking it against the owner's signing records.
The integration returned HTTP 403 for repository administration and environment-variable access,
so it could not apply settings or inspect/set the release variable. Private reporting was already enabled.
Two branches whose SHAs exactly matched merged PR heads were removed after a fresh SHA check:
`ci/required-checks-always-report` and `feature/backend`. Other historical branches were preserved. Other settings
and the production signing setup must be checked against GitHub, not inferred from this document.
No release is published merely by opening or reviewing these PRs.

## Validation matrix

| Change or failure | Expected outcome |
| --- | --- |
| Documentation only | Security/conventions/gate run; project builds explicitly skipped |
| Android only | Core, app, visual verification and API 35 smoke run |
| Server only | Server checks/image and Android/server integration smoke run |
| Web only | Web tests/build run |
| CI tooling changes or uncertain diff | All projects run |
| Failed/cancelled scan or unexpectedly skipped selected job | Gate fails |
| Missing baseline or unexpected image difference | Visual verification fails with comparison artifacts |
| Missing key, wrong certificate or mismatched version | Official release remains unpublished |
| Server startup, image validation or package verification fails | Previous latest release stays available |
| Interrupted draft build | Rerun the same release; reuse the tag, never overwrite published files |

Local validation passed the Kotlin core tests, 140 app unit/screenshot tests, formatting, lint,
debug and release builds. All 44 baseline hashes matched across two recording runs; changing one
pixel failed verification and restoring it passed. The downloaded v0.10.0 APK/AAB passed signature,
version and checksum verification. Missing signing configuration and a wrong certificate were rejected.
The tooling tests also cover forward-only release promotion so retrying an older version cannot move
`latest` backwards. Server CI smoke-tests the built image with a disposable PostgreSQL instance.

The Python gate tests cover selection and rejection of failures/skips. Live checks complement them;
GitHub settings, fork-token restrictions and signed upgrade behavior require their own acceptance runs.
The smoke suite verifies Activity recreation and a fresh SQLite read; a real process-death/restore
check is still part of the device acceptance checklist, not implied by that test.

## Requirements and backlog

Every new issue must state the problem, observable acceptance criteria, testing expectations and
related requirement IDs. IDs alone are insufficient when the PRD is outside the repository.
The [Repository hardening milestone](https://github.com/MohdFarag/bawsala/milestone/1) tracks
these changes in #86–#91, assigned to the owner. Product backlog issues remain open;
#77 is not complete until its full device acceptance criteria are demonstrated.
