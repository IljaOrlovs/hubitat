# Hubitat

Drivers and apps for [Hubitat Elevation](https://hubitat.com): MagicHome, Lightify, Russound, Dream Screen, Heatit, Zipato, Unifi Access, Slack, Circadian Daylight and more.

## Install
With [Hubitat Package Manager](https://hubitatpackagemanager.hubitatcommunity.com/): *Install* → *From a URL* → `https://raw.githubusercontent.com/IljaOrlovs/hubitat/master/Packages/<Name>/packageManifest.json` (or add `repository.json` as a custom repository: *Install* → *Search by Repository*). HPM reads from `master`, so changes are installable only once merged.

Manually: *Drivers Code* / *Apps Code* → *Import* with the file's `importUrl`, or paste it in.

## Develop
Requires Docker only.

```bash
make test    # Spock + hubitat_ci specs
make coverage # tests + coverage ratchet
make lint    # CodeNarc (fails on priority 1-2)
make format  # auto-format with npm-groovy-lint
```

See [AGENTS.md](AGENTS.md) for layout, conventions, docs links and known issues.

## License
Apache-2.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE). Changes: [CHANGELOG](CHANGELOG.md). Contributing: [CONTRIBUTING](CONTRIBUTING.md).
