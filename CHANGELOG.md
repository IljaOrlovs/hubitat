# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions are per package (see `Packages/*/packageManifest.json`), semver.

## Unreleased

### Added
- Tooling: `make check|lint|format|manifests`, CI, editorconfig, CodeNarc and npm-groovy-lint config.
- Hubitat Package Manager manifests (`repository.json`, `Packages/`), Apache-2.0 `LICENSE`.

### Changed
- All files and directories renamed to PascalCase without spaces; `importUrl`s now point at this repo.
- Whole-repo whitespace normalization (tabs to 4 spaces, trailing whitespace, final newline).
- Access Doorbell: uses the ping-interval setting, cancels stale reconnects, sets `numberOfButtons`, hides the API token.

### Fixed
- `rgbToHSV` returned hue 0 for most colors (float/BigDecimal comparison); the same bug was in MagicHome RGBW Legacy.
- `hsvToRGB` returned black at maximum hue instead of wrapping to red.
- Circadian Daylight: saturation was scaled by 100/360 when converting RGB to HSV.
- Access Doorbell: undefined `refresh()` in `poll()`, command declared in the wrong block, `release()` ignored the button number, non-JSON messages threw.
- `importURL` typo (ignored by Hubitat) corrected to `importUrl`.
