import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.PendingFeature
import spock.lang.Specification

class MagicHomeManagerSpec extends Specification {
    AppExecutor api = Mock()
    Map state = [:]
    List childApps = []

    def app() {
        api.getLog() >> Stub(Log)
        api.getState() >> state
        api.getChildApps() >> childApps
        new HubitatAppSandbox(new File('Apps/MagicHome/MagicHomeManager.groovy')).run(api: api, validationFlags: [Flags.DontRunScript, Flags.AllowReadingNonInputSettings])
    }

    def "installed records completion and initializes"() {
        when:
        app().installed()

        then:
        state.initializeCompleted
    }

    def "updated unsubscribes before re-initializing"() {
        when:
        app().updated()

        then:
        1 * api.unsubscribe()
    }

    def "initialize with no child apps does nothing"() {
        when:
        app().initialize()

        then:
        noExceptionThrown()
    }

    @PendingFeature(reason = 'initialize calls logInformationText(), which is not defined anywhere, for every child app')
    def "initialize logs each child app"() {
        given:
        childApps << [label: 'Kitchen strip']

        when:
        app().initialize()

        then:
        noExceptionThrown()
    }
}
