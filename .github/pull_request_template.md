<!-- Title: Conventional Commits, e.g. "feat(android): sign in and sync". It becomes the commit on main. -->

## What changed

<!-- One or two sentences on the change and why. -->

## PRD coverage

<!-- Link an issue with observable acceptance criteria; include affected requirement IDs, e.g. CAP02. "None" for tooling. IDs alone are not enough when the PRD is private. -->

## How it was tested

<!-- Tick what applies; CI runs these for the touched projects. -->

- [ ] Android: `./gradlew -p core test ktfmtCheck` and `./gradlew :app:ktfmtCheck :app:verifyRoborazziDebug :app:lintDebug`
- [ ] Server: `npm run format:check && npm run lint && npm run typecheck && npm test`
- [ ] Web: `npm run format:check && npm test && npm run build`
- [ ] Tried on a device or emulator (which one?)

## Screenshots

<!-- For UI changes: before / after, including Arabic (RTL) where relevant. -->
