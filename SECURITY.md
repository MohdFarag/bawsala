# Security policy

## Supported versions

Security fixes target the latest published Bawsala release. Older versions do not receive
backported fixes; upgrade to the latest release. Fixes are distributed in the next release.

## Report a vulnerability privately

Use [GitHub private vulnerability reporting](https://github.com/MohdFarag/bawsala/security/advisories/new).
Include the affected version, reproduction steps, expected impact, and a minimal example.
Do not publish exploit details, personal information, credentials or tokens in public issues or PRs.
If the reporting form is unavailable, contact [the repository owner](https://github.com/MohdFarag)
through a private contact method listed on their profile; do not disclose the vulnerability publicly.

## Automated checks

CI scans Git history with gitleaks, audits dependencies, checks workflows with actionlint and
zizmor, and runs CodeQL on TypeScript/JavaScript and Actions. Trusted CodeQL results appear in
GitHub's Security tab; fork PRs retain scanning and downloadable SARIF reports without write access.
Missing scan output and findings fail validation. Dependabot proposes dependency updates weekly.

## Data handling

Data stays on the device unless sync is enabled. Google tokens are encrypted on the server and
are never exposed to the app UI, exports or logs. Permissions are requested only when a feature
needs them. Security tools complement review and device testing; passing checks are not a security guarantee.
