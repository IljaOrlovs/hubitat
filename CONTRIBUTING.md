# Contributing

Requirements: Docker, make. Run `make setup` once to enable the pre-commit hook.

| Command | Does |
|---|---|
| `make test` | Spock + hubitat_ci specs (validates every script, behaviour tests) |
| `make coverage` | `make test` + per-file coverage ratchet (`tests/coverage-baseline.json`) |
| `make lint` | CodeNarc, fails on P1/P2 |
| `make format` | auto-format (npm-groovy-lint) |
| `make manifests` | regenerate `repository.json` and `Packages/*/packageManifest.json` |

CI runs these and fails on any resulting diff.

## Conventions
- PascalCase file and directory names, no spaces. A new file needs an `importUrl` (this repo's raw URL) and gets picked up by `make manifests`.
- Pull logic out of Hubitat callbacks into plain methods so it can be tested; see `tests/spec/AccessDoorbellSpec.groovy` for how to mock the Hubitat API. Coverage may not drop; after adding tests run `make coverage-update` and commit the baseline.
- Add a bullet under *Unreleased* in `CHANGELOG.md`. To ship a change, bump the package `version` and `dateReleased` in its `packageManifest.json`.
- Commits: imperative subject line, one logical change each. Formatting-only changes go in their own commit.
- Never commit credentials.
