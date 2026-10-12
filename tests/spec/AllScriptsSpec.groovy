import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Every driver/app must compile, run its metadata and pass hubitat_ci's definition/preferences/API validation.
 * Scripts.LEGACY relaxes checks the old code predates (or that hubitat_ci 0.17 lags the platform on). Tighten it, never widen it.
 */
class AllScriptsSpec extends Specification {
    static List<File> scripts(String dir) {
        def out = []
        new File(dir).eachFileRecurse { if (it.name.endsWith('.groovy')) out << it }
        out.sort()
    }

    @Unroll
    def "driver validates: #file"() {
        expect:
        new HubitatDeviceSandbox(file).run(validationFlags: Scripts.flagsFor(file))

        where:
        file << scripts('Drivers')
    }

    @Unroll
    def "app validates: #file"() {
        expect:
        new HubitatAppSandbox(file).run(validationFlags: Scripts.flagsFor(file) + [Flags.DontValidateDefinition])

        where:
        file << scripts('Apps')
    }
}
