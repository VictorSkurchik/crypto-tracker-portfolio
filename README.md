# Crypto Portfolio Tracker

A Kotlin Multiplatform app for tracking your crypto portfolio in one place: centralized exchange
balances, on-chain wallet balances, and manually-tracked custom assets. Android and Desktop are
the primary targets; iOS is supported but secondary.

There is no backend — everything runs on-device. Exchange API keys are stored using
platform-appropriate secure storage (Android Keystore-backed encrypted preferences, iOS Keychain,
an encrypted local file on Desktop) and are only ever used to sign requests directly from the app.

## Features

- **Exchanges** — Binance, OKX, Bybit, Bitget, via a read-only API key/secret (+ passphrase for
  OKX/Bitget).
- **On-chain wallets** — Ethereum, Optimism, Arbitrum (via Etherscan's V2 API), Ton (via TON
  Center), and Tron (via TronGrid), by wallet address.
- **Custom assets** — anything off-API, with a fixed price or live pricing from CoinMarketCap.
- A portfolio dashboard aggregating everything into a total value, broken down by account and by
  asset.

## Module structure

- `core/model` — domain types and the provider/connector abstractions (`OnChainProvider`,
  `ExchangeConnector`, `PriceProvider`) shared by everything else.
- `core/network` — Ktor HTTP client plus every concrete provider/connector implementation.
- `core/secrets` — per-platform secure storage for exchange credentials.
- `core/database` — Room, the on-device cache.
- `core/datastore`, `core/data` — small app preferences, and the repositories tying
  network+database+secrets together.
- `core/designsystem`, `core/navigation`, `core/di` — Compose theme, navigation routes, Koin
  wiring.
- `feature/*` — one module per screen area: `portfolio`, `wallets`, `exchanges`,
  `customassets`, `settings`.
- `app/shell` — the root `CptApp()` composable and the iOS framework entry point.
- `androidApp`, `desktopApp`, `iosApp` — the platform application shells.
- `build-logic/convention` — Gradle convention plugins (`cpt.*`) used by every module.

## Running the apps

- Android: `./gradlew :androidApp:assembleDevDebug` (flavors: `dev`, `stage`, `prod`; build
  types: `debug`, `release`)
- Desktop: `./gradlew :desktopApp:run`
- iOS: open [`iosApp/iosApp.xcodeproj`](./iosApp/iosApp.xcodeproj) in Xcode and run it from there.

## Conventions

- Commits follow [Conventional Commits](https://www.conventionalcommits.org/).
- Branching: `main` (stable), `develop` (integration), `feature/<name>` (off `develop`).
- Licensed under [Apache 2.0](./LICENSE).
