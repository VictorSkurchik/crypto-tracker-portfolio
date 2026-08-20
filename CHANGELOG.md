# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and commit messages follow
[Conventional Commits](https://www.conventionalcommits.org/) (see the README).

## [Unreleased]

### Added

- Custom design-system theme (light/dark colors, typography) and an original monogram icon set for
  exchanges, chains, and tokens, replacing the default Material look.
- Adaptive window-size and foldable-aware navigation (compact/medium/expanded, hinge-aware
  side-by-side layout).
- Pull-to-refresh on the portfolio dashboard, with per-asset breakdown and a last-refreshed
  timestamp.
- Live API-call verification of exchange credentials before saving.
- Wallet-address format and custom-asset input validation before saving.
- CI: `./gradlew check` (detekt, ktlint, Android lint, unit tests) runs on every push/PR.

### Fixed

- TON jetton symbol/decimals now resolved via jetton master lookup instead of guessed.

### Changed

- App version (Android `versionName`/`versionCode`, Desktop `packageVersion`, iOS
  `MARKETING_VERSION`/`CURRENT_PROJECT_VERSION`) now comes from a single source of truth,
  `version.xcconfig` at the repo root, instead of three independently hardcoded values.

## [0.1.0] - initial scaffold

- Initial Kotlin Multiplatform client: exchange/wallet/custom-asset tracking, on-device secure
  storage, Room cache, Compose Multiplatform UI across Android/Desktop/iOS.
