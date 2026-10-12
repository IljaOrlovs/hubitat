# Needs only Docker. Hubitat runs Groovy 2.4; syntax checked here with same Groovy line.
GROOVY = docker run --rm -v "$(CURDIR)":/w -w /w groovy:2.4-jre8
GROOVY3 = docker run --rm -v "$(CURDIR)":/w -w /w groovy:3.0.9-jdk11

.PHONY: check lint format manifests setup
check:
	$(GROOVY) groovy -Dfile.encoding=UTF-8 tests/check.groovy .
lint:
	$(GROOVY3) groovy -Dfile.encoding=UTF-8 tests/lint.groovy
format:
	-docker run --rm -v "$(CURDIR)":/work -w /work nvuillam/npm-groovy-lint:v18.0.0 --format --noserver --files "**/*.groovy" --ignorepattern "tests/**"
	$(MAKE) check
manifests:
	$(GROOVY) groovy -Dfile.encoding=UTF-8 tools/genManifests.groovy
setup:
	git config core.hooksPath .githooks
