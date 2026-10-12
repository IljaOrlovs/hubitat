// Run: make check   (syntax-parses every driver/app, then unit-tests the pure helpers)
def root = new File(args ? args[0] : '.')
def sources = []
root.eachFileRecurse { if (it.name.endsWith('.groovy') && !it.path.contains('/tests/') && it.name != 'codenarc.groovy') sources << it }

int failed = 0
sources.sort().each { f ->
    try {
        def cu = new org.codehaus.groovy.control.CompilationUnit()
        cu.addSource(f)
        cu.compile(org.codehaus.groovy.control.Phases.CONVERSION) // syntax only: hubitat.* classes don't resolve outside the hub
    } catch (e) { failed++; println "SYNTAX ${f}: ${e.message.readLines().find { it.contains('@ line') || it.contains('Unexpected') } ?: e.message.readLines()[1]}" }
}
println "parsed ${sources.size()} files, ${failed} syntax errors"

// --- color helpers (Methods/ files are plain scripts: load and call) ---
def load = { String n -> new GroovyShell().parse(new File(root, "Methods/${n}.groovy")) }
def hsv = load('hsvToRGB'), rgb = load('rgbToHSV')
def near = { a, b -> Math.abs(a - b) <= 1 }
def tests = [
    'red at hue 0'          : { hsv.hsvToRGB(0, 100, 100).with { near(red, 255) && near(green, 0) && near(blue, 0) } },
    'hue max wraps to red'  : { hsv.hsvToRGB(100, 100, 100).with { near(red, 255) && near(green, 0) } },
    'hue 360 high-res = red': { hsv.hsvToRGB(360, 100, 100, 'high').with { near(red, 255) && near(blue, 0) } },
    'green at 120'          : { hsv.hsvToRGB(120, 100, 100, 'high').with { near(green, 255) && near(red, 0) } },
    'rgb->hsv blue'         : { rgb.rgbToHSV(0, 0, 255, 'high').with { near(hue, 240) && near(saturation, 100) } },
    'round trip'            : { def c = rgb.rgbToHSV(10, 200, 90, 'high'); hsv.hsvToRGB(c.hue, c.saturation, c.value, 'high').with { near(red, 10) && near(green, 200) && near(blue, 90) } },
]
tests.each { name, t -> boolean ok; try { ok = t() } catch (e) { ok = false; println e }; if (!ok) failed++; println "${ok ? 'PASS' : 'FAIL'} ${name}" }
System.exit(failed ? 1 : 0)
