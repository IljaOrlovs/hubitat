# Hubitat

Drivers and apps for [Hubitat Elevation](https://hubitat.com): MagicHome, Lightify, Russound, Dream Screen, Heatit, Zipato, Unifi Access, Slack, Circadian Daylight and more.

## Install
Import a file via the hub's *Drivers Code* / *Apps Code* → *Import* using its raw GitHub URL (the `importUrl` in each file's metadata), or paste it in.

## Develop
Requires Docker only.

```bash
make check   # syntax-parse all files + unit tests for Methods/
make lint    # CodeNarc (fails on priority 1-2)
make format  # auto-format with npm-groovy-lint
```

See [AGENTS.md](AGENTS.md) for layout, conventions, docs links and known issues.

## Licence
Apache 2.0 (see file headers).
