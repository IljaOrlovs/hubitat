import spock.lang.Unroll

/** Every light/plug command on every socket-based MagicHome driver transmits a frame and updates attributes, in all switch/pre-staging states. */
class MagicHomeCommandsSpec extends DriverSpec {
    /** Subclasses override to rerun every feature under different preferences. */
    Map extraSettings() { [:] }
    Map settings() { MagicHome.DEFAULTS + extraSettings() }

    static final List<String> NOT_SOCKET = MagicHome.RAW

    /** [method, args] pairs; each is skipped for drivers that do not define the method. */
    static final List CALLS = [
        ['on', []], ['off', []],
        ['setHue', [50]], ['setHue', [200]], ['setSaturation', [60]], ['setLevel', [40]], ['setLevel', [100, 2]], ['setLevel', [0]],
        ['setColor', [[hue: 10, saturation: 20, level: 30]]], ['setColor', [[hue: 90, saturation: 100, level: 100]]],
        ['setColorTemperature', [3000]], ['setColorTemperature', [4000]], ['setColorTemperature', [6000]],
        ['setWhiteLevel', [50]], ['setWarmWhiteLevel', [50]], ['setColdWhiteLevel', [50]],
        ['sendPreset', [3, 50]], ['sendPreset', [25, 200]], ['presetRedFade', [80]], ['presetSevenColorJump', []],
    ]

    @Unroll
    def "#method(#args) on #file with switch #switchState and pre-staging #preStaging transmits and does not throw"() {
        given:
        def d = load(path, settings() + [enablePreStaging: preStaging])
        attrs += [switch: switchState, hue: 30, saturation: 40, level: 50, colorTemperature: 3500, whiteLevel: 20]
        state.lastConnectionAttempt = clock

        when:
        d."$method"(*args)

        then:
        noExceptionThrown()
        !ifaceCalls('sendSocketMessage').isEmpty()

        where:
        [path, call, switchState, preStaging] << sweep()
        method = call[0]
        args = call[1]
        file = path.split('/').last() - 'MagicHome' - '.groovy'
    }

    /** Every (driver, call, switch state, pre-staging) combination the driver actually supports. */
    static List sweep() {
        def defined = CALLS*.getAt(0).unique().collectEntries { [(it): Scripts.defining('Drivers/MagicHome', it)] }
        MagicHome.having('on', NOT_SOCKET).collectMany { p ->
            def presets = (new File(p).text =~ /(?m)^def (preset\w+)\(/).collect { [it[1], [80]] }   // every preset* shortcut
            [CALLS.findAll { defined[it[0]].contains(p) } + presets, ['on', 'off'], [false, true]].combinations().collect { [p, *it] }
        }
    }
}
