# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions are per package (see `Packages/*/packageManifest.json`), semver.

## Unreleased

### Added
- Tests: ~2,400 Spock cases for the MagicHome family (all 11 files, 36-97% line coverage) that also document 20+ confirmed bugs as pending specs; Spock + hubitat_ci specs validating all 45 scripts, behaviour specs for Access Doorbell and the color helpers, JaCoCo coverage ratchet.
- Tooling: `make test|coverage|lint|format|manifests`, CI, editorconfig, CodeNarc and npm-groovy-lint config.
- Hubitat Package Manager manifests (`repository.json`, `Packages/`), Apache-2.0 `LICENSE`.

### Changed
- `MagicHomeManager` is an app and moved from `Drivers/MagicHome` to `Apps/MagicHome`.
- All files and directories renamed to PascalCase without spaces; `importUrl`s now point at this repo.
- Whole-repo whitespace normalization (tabs to 4 spaces, trailing whitespace, final newline).
- Access Doorbell: uses the ping-interval setting, cancels stale reconnects, sets `numberOfButtons`, hides the API token.

### Fixed
- MagicHome RGBW Legacy: removed a dead line in the embedded `hsvToRGB` that assigned an undeclared variable.
- `rgbToHSV` returned hue 0 for most colors (float/BigDecimal comparison); the same bug was in MagicHome RGBW Legacy.
- `hsvToRGB` returned black at maximum hue instead of wrapping to red.
- Circadian Daylight: saturation was scaled by 100/360 when converting RGB to HSV.
- Access Doorbell: undefined `refresh()` in `poll()`, command declared in the wrong block, `release()` ignored the button number, non-JSON messages threw.
- `importURL` typo (ignored by Hubitat) corrected to `importUrl`.
