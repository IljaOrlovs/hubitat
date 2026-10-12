// Regenerates repository.json and Packages/<Name>/packageManifest.json for Hubitat Package Manager.
// Run: make manifests. Existing version/dateReleased are kept; bump them by hand when releasing a package.
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def raw = 'https://raw.githubusercontent.com/IljaOrlovs/hubitat/master/'
def author = 'Ilja Orlovs'
def root = new File('.')
def slurp = new JsonSlurper()

// package name = first dir under Apps/ or Drivers/ (Utilities/ uses its subdir); loose files use their own name
def packages = new TreeMap()
['Apps', 'Drivers'].each { kind ->
    new File(root, kind).eachFileRecurse { f ->
        if (!f.name.endsWith('.groovy')) return
        def parts = root.toPath().relativize(f.toPath()).toList()*.toString()
        def key = parts.size() == 2 ? f.name - '.groovy' : (parts[1] == 'Utilities' ? (parts.size() == 3 ? f.name - '.groovy' : parts[2]) : parts[1])
        def text = f.text
        def def_ = (text =~ /(?s)definition\s*\((.*?)\)\s*\{?/)
        def body = def_.find() ? def_.group(1) : ''
        def pick = { String k -> (body =~ /${k}:\s*["']([^"']+)["']/).with { it.find() ? it.group(1) : null } }
        def entry = [id: UUID.nameUUIDFromBytes(parts.join('/').bytes).toString(), name: pick('name') ?: f.name - '.groovy',
                     namespace: pick('namespace') ?: 'unknown', location: raw + parts.collect { URLEncoder.encode(it, 'UTF-8') }.join('/'), required: true]
        if (kind == 'Apps') { entry.oauth = false; entry.primary = !(body =~ /parent:/).find() }
        packages.get(key, [apps: [], drivers: []])[kind == 'Apps' ? 'apps' : 'drivers'] << entry
        }
    }

def repo = [author: author, githubUrl: 'https://github.com/IljaOrlovs/hubitat', packages: []]
packages.each { name, p ->
    def dir = new File(root, "Packages/${name}"); dir.mkdirs()
    def mf = new File(dir, 'packageManifest.json')
    def old = mf.exists() ? slurp.parse(mf) : [:]
    p.apps.sort { it.name }; p.drivers.sort { it.name }
    def manifest = [packageName: name, author: author, version: old.version ?: '0.1.0', minimumHEVersion: '2.3.0',
                    dateReleased: old.dateReleased ?: '2026-10-12']
    if (p.apps) manifest.apps = p.apps
    if (p.drivers) manifest.drivers = p.drivers
    mf.text = JsonOutput.prettyPrint(JsonOutput.toJson(manifest)) + '\n'
    repo.packages << [name: name, category: p.apps ? 'Control' : 'Integrations', location: raw + "Packages/${name}/packageManifest.json",
                      description: "${name} for Hubitat Elevation", tags: []]
}
new File(root, 'repository.json').text = JsonOutput.prettyPrint(JsonOutput.toJson(repo)) + '\n'
println "wrote ${packages.size()} packages"
