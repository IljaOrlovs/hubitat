// Aggregates JaCoCo CSV into per-file line coverage and enforces tests/coverage-baseline.json (a ratchet: may only go up).
// Usage: groovy tools/coverage.groovy [--update]
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def update = args.contains('--update')
def csv = new File('build/reports/jacoco/test/jacocoTestReport.csv')
def baselineFile = new File('tests/coverage-baseline.json')
def baseline = baselineFile.exists() ? new JsonSlurper().parse(baselineFile) : [:]

def sources = [:]   // script class name -> path
['Apps', 'Drivers', 'Methods'].each { d -> new File(d).eachFileRecurse { if (it.name.endsWith('.groovy')) sources[it.name - '.groovy'] = it.path } }

def stats = sources.values().collectEntries { [(it): [missed: 0, covered: 0]] }
csv.readLines().drop(1).each { l ->
    def c = l.split(',')
    def file = sources[c[2].split('\\$')[0]]
    if (!file) return
    stats[file].missed += c[7] as int
    stats[file].covered += c[8] as int
}

def pct = { s -> (s.missed + s.covered) ? Math.floor(100.0 * s.covered / (s.missed + s.covered)) as int : 100 }
int bad = 0
long tm = 0, tc = 0
def current = new TreeMap()
stats.sort().each { file, s ->
    tm += s.missed; tc += s.covered
    int p = pct(s); current[file] = p
    int floor = baseline[file] ?: 0
    if (p < floor) { bad++; println "REGRESSED ${file}: ${p}% < baseline ${floor}%" }
    println String.format('%5d%%  %5d/%-5d  %s', p, s.covered, s.covered + s.missed, file)
}
println String.format('TOTAL  %.1f%%  (%d of %d lines)', 100.0 * tc / (tc + tm), tc, tc + tm)
if (update) {
    baselineFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(current)) + '\n'
    println "baseline updated"
    System.exit(0)
}
System.exit(bad ? 1 : 0)
