# AGENTS.md

Hubitat Elevation drivers and apps (Groovy 2.4 on the hub). Distributed through [Hubitat Package Manager](https://hubitatpackagemanager.hubitatcommunity.com/) (`repository.json` + `Packages/*/packageManifest.json`, read from `master`); there is no build. Manual install is *Apps/Drivers Code → Import* via each file's `importUrl`. No Hubitat bundle: it has no update path.

## Layout
- `Apps/`, `Drivers/` — one dir per integration; parent/child apps are separate files. Files and dirs are PascalCase with no spaces or punctuation (`MagicHomeBulbRgbw.groovy`); each `importUrl` points at this repo's raw URL for the file, so update it on any rename.
- `Methods/` — standalone helpers, deprecated in favour of Hubitat's built-in `ColorUtils`.
- `Examples/` — primers. `tests/` — `spec/` (Spock specs), `support/` (stubs for newer Hubitat classes), `lint.groovy` (CodeNarc).

## Commands (Docker only, no local JVM)
- `make test` — Gradle + Spock + [hubitat_ci](https://github.com/biocomp/hubitat_ci) (docs: docs/how_to_test.md in that repo): `tests/spec/AllScriptsSpec` compiles and validates every script; other specs test behaviour with `Mock(DeviceExecutor)` / `Mock(AppExecutor)`. Helpers newer than hubitat_ci 0.17 (`ColorUtils`, `HexUtils`, `InterfaceUtils`) are stubbed in `tests/support/me/biocomp/hubitat_ci/api/helper` (hubitat_ci maps `hubitat.X` to `me.biocomp.hubitat_ci.api.X`).
- `make coverage` — `make test` plus a JaCoCo line-coverage ratchet (`tests/coverage-baseline.json`, per file; may only go up). `make coverage-update` after adding tests. Run it and commit the new baseline.
- `make lint` — CodeNarc with `codenarc.groovy`. Fails on any P1 or P2 violation (baseline is clean).
- `make format` — npm-groovy-lint auto-format (config: `.groovylintrc.json`),  Brace-adding, quote and unused-code fixers are deliberately off: they changed structure (dropped a `}` in Simple Sprinklers) or churned quotes.
- `make manifests` — regenerates HPM `repository.json` and `Packages/*/packageManifest.json` from the files' `definition()` blocks (keeps existing version/date).
- `make setup` — enables `.githooks/pre-commit`. See CONTRIBUTING.md.
- CI (`.github/workflows/ci.yml`) runs both.

## Conventions
- 4 spaces, LF, final newline (`.editorconfig`); all `.groovy` files are normalized to this.
- Prefer built-ins over copies: `hubitat.helper.ColorUtils`, `HexUtils`. ~15 drivers still carry their own `clamp`, `calculateChecksum`, `logDebug`; Hubitat has no shared-library import except Libraries code, so extract to a Hubitat Library (`#include`) rather than copy again.
- Every new driver/app: `importUrl` (lowercase `l` — `importURL` is silently ignored), `updated()` that calls `unschedule()` and re-inits, no secrets in source, and logging gated by a preference.
- Float vs BigDecimal: `BigDecimal == float` is false in Groovy; cast before comparing (this broke `rgbToHSV`).

## Docs sources
- Maker API: https://docs2.hubitat.com/en/apps/maker-api — the page is JS-rendered; WebFetch returns only the title, so open it in the browser pane. Start from the docs site root for driver/app developer docs.
- Color helpers: https://docs.hubitat.com/index.php?title=ColorUtils_Object
- Add new sources here as you use them.

## Known open issues (not yet fixed)
- `Drivers/Slack/SlackNotifier.groovy`: `updated()`/`initialize()` empty; webhook default `https://...` is not validated.
- `Apps/Utilities/RootinTootinSelfRebootin.groovy`: credentials are `@Field` constants edited in source; move to preferences.
- Unguarded `.toInteger()` on external input (~170 sites), mostly Lightify/MagicHome.

## Known bugs (each has a `@PendingFeature` spec stating the correct behaviour)
`grep -rn PendingFeature tests/spec` lists them. When you fix one the spec turns red: delete the annotation. Highlights: `logdebug()` typo breaks connect on two MagicHome bulbs; `limit()`/`normalizePercent()` results are discarded so input is never clamped; WS28xx partial colour updates, `setColorTemperature`, `setEffectSpeed`, next/previous effect and custom effects are broken; the RGB+WW/CW bulb throws parsing any frame with white LEDs lit; `MagicHomeManager.initialize` calls undefined `logInformationText`; `Heatit` off-mode sends mode 11.

## Testing notes
- `AllScriptsSpec.KNOWN` lists scripts exempt from hubitat_ci's metadata/preferences validation (it lags the platform, or the script breaks a rule). They are still compiled and instantiated; delete a line once the script passes.
- To test a script: `new HubitatDeviceSandbox(new File('Drivers/...')).run(api: apiMock, userSettingValues: [...], validationFlags: [...])`; see `tests/spec/AccessDoorbellSpec.groovy` for a full example (mocking `getInterfaces()`, `sendEvent`, `runIn`).
- Pull logic out of callbacks into plain methods so it is testable; hubitat_ci forwards every unknown call to the `api` mock, so unexpected API use shows up as an unmatched interaction.
- `DriverSpec` (tests/support) is the base for driver specs: `load(path, settings)`, `state`/`attrs`/`events` maps, `raw`/`ws` socket mocks, `ifaceCalls('sendSocketMessage')`. `settings` reaches device scripts via a user setting named `settings` (hubitat_ci has no `settings` object for devices).
- Specs are data-driven across the MagicHome family (`MagicHome.having(method)` lists the drivers that define a method); the `*Quiet*` subclasses rerun them with different preferences to cover the other branches.
