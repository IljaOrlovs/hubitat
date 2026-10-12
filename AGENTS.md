# AGENTS.md

Hubitat Elevation drivers and apps (Groovy 2.4 on the hub). Code is pasted/imported into the hub; there is no build.

## Layout
- `Apps/`, `Drivers/` — one dir per integration; parent/child apps are separate files. Files and dirs are PascalCase with no spaces or punctuation (`MagicHomeBulbRgbw.groovy`); each `importUrl` points at this repo's raw URL for the file, so update it on any rename.
- `Methods/` — standalone helpers, deprecated in favour of Hubitat's built-in `ColorUtils`. Only place with unit tests.
- `Examples/` — primers. `tests/` — `check.groovy` (syntax + unit tests), `lint.groovy` (CodeNarc).

## Commands (Docker only, no local JVM)
- `make check` — syntax-parses every `.groovy` (CONVERSION phase; `hubitat.*` won't resolve off-hub) and runs the color tests.
- `make lint` — CodeNarc with `codenarc.groovy`. Fails on any P1 or P2 violation (baseline is clean).
- `make format` — npm-groovy-lint auto-format (config: `.groovylintrc.json`), then re-runs `make check`. Brace-adding, quote and unused-code fixers are deliberately off: they changed structure (dropped a `}` in Simple Sprinklers) or churned quotes.
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
