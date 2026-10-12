@Grab('org.codenarc:CodeNarc:3.0.1')
@Grab('org.slf4j:slf4j-simple:2.0.13')
import org.codenarc.CodeNarcRunner
import org.codenarc.report.TextReportWriter
def runner = new CodeNarcRunner(
    sourceAnalyzer: new org.codenarc.analyzer.FilesystemSourceAnalyzer(baseDirectory: '.', includes: '**/*.groovy', excludes: 'tests/**,codenarc.groovy'),
    ruleSetFiles: 'file:codenarc.groovy',
    reportWriters: [new TextReportWriter(outputFile: '/dev/stdout')])
def r = runner.execute()
System.exit((r.getNumberOfViolationsWithPriority(1) + r.getNumberOfViolationsWithPriority(2)) ? 1 : 0)
