# Needs only Docker. Containers run as your uid so files they write stay yours (matters on Linux/CI).
UID := $(shell id -u)
GID := $(shell id -g)
RUN = docker run --rm --user $(UID):$(GID) -e HOME=/tmp -v "$(CURDIR)":/w -w /w
GROOVY = $(RUN) -e JAVA_OPTS=-Duser.home=/tmp groovy:2.4-jre8
GROOVY3 = $(RUN) -e JAVA_OPTS=-Duser.home=/tmp groovy:3.0.9-jdk11
GRADLE = $(RUN) -e GRADLE_USER_HOME=/w/build/gradle-home -e GRADLE_OPTS=-Duser.home=/tmp -e JAVA_TOOL_OPTIONS=-Duser.home=/tmp gradle:7.6.4-jdk8 gradle --no-daemon -q

.PHONY: test coverage coverage-update lint format manifests setup
# Spock + hubitat_ci specs (tests/spec): validate every script, then run behaviour specs. Writes build/reports/jacoco.
test:
	$(GRADLE) test jacocoTestReport
# Fails if any file's line coverage drops below tests/coverage-baseline.json (a ratchet).
coverage: test
	$(GROOVY) groovy tools/coverage.groovy
coverage-update: test
	$(GROOVY) groovy tools/coverage.groovy --update
lint:
	$(GROOVY3) groovy -Dfile.encoding=UTF-8 tests/lint.groovy
format:
	-$(RUN) -e JAVA_OPTS=-Duser.home=/tmp nvuillam/npm-groovy-lint:v18.0.0 --format --noserver --files "**/*.groovy" --ignorepattern "tests/**"
manifests:
	$(GROOVY) groovy -Dfile.encoding=UTF-8 tools/genManifests.groovy
setup:
	git config core.hooksPath .githooks
