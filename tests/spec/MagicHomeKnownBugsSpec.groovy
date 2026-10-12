import spock.lang.PendingFeature
import spock.lang.Unroll

/**
 * Bugs found while writing these specs, left unfixed on purpose (no functional changes yet). Each test states the CORRECT behaviour
 * and is @PendingFeature: it is reported as "expected to fail" today and turns red the day the bug is fixed, at which point
 * delete the annotation (and move the driver into the shared specs it is currently excluded from).
 */
class MagicHomeKnownBugsSpec extends DriverSpec {
    @PendingFeature(reason = 'connectDevice calls undefined logdebug() (lower-case d) so every connection attempt throws')
    @Unroll
    def "connectDevice works: #path"() {
        given:
        def d = load(path, MagicHome.DEFAULTS)
        state.lastConnectionAttempt = clock

        when:
        d.connectDevice([firstRun: true])

        then:
        ifaceCalls('socketConnect').size() == 1

        where:
        path << MagicHome.DRIVERS.findAll { p -> MagicHome.LOG_TYPO.any { p.endsWith("MagicHome${it}.groovy") } }
    }

    @PendingFeature(reason = 'powerOnWithChanges reads the nonexistent "status" attribute, so it re-sends on() even when the light is already on')
    def "WW/CW bulb does not re-send on when already on"() {
        given:
        def d = load('Drivers/MagicHome/MagicHomeBulbWwCwCct.groovy', MagicHome.DEFAULTS)
        attrs.switch = 'on'

        when:
        d.powerOnWithChanges()

        then:
        events.findAll { it.name == 'switch' }.isEmpty()
    }

    @PendingFeature(reason = 'limit(hue) discards its return value, so out-of-range input is not clamped')
    @Unroll
    def "#setter clamps out-of-range input to 0-100 (RGB controller)"() {
        given:
        def d = load('Drivers/MagicHome/MagicHomeControllerRgb.groovy', MagicHome.DEFAULTS)
        attrs += [switch: 'on', hue: 10, saturation: 10, level: 10]

        when:
        d."$setter"(500)

        then:
        sent(attribute) == 100

        where:
        setter          | attribute
        'setHue'        | 'hue'
        'setSaturation' | 'saturation'
        'setLevel'      | 'level'
    }

    @PendingFeature(reason = 'sendPreset publishes currentPreset as preset+36 (the wire value), not the 1-20 the attribute documents')
    def "currentPreset attribute reports the preset number (RGB controller)"() {
        given:
        def d = load('Drivers/MagicHome/MagicHomeControllerRgb.groovy', MagicHome.DEFAULTS)
        attrs.switch = 'on'

        when:
        d.sendPreset(3, 50)

        then:
        sent('currentPreset') == 3
    }
}

/** What the two logdebug()-typo drivers do TODAY: everything up to the typo runs, then connectDevice throws. Delete with the typo. */
class MagicHomeLogTypoTodaySpec extends DriverSpec {
    static final List<String> PATHS = MagicHome.DRIVERS.findAll { p -> MagicHome.LOG_TYPO.any { p.endsWith("MagicHome${it}.groovy") } }

    @Unroll
    def "connectDevice opens the socket, then throws MissingMethodException at the typo: #path"() {
        given:
        def d = load(path, MagicHome.DEFAULTS)
        state.lastConnectionAttempt = clock

        when:
        d.connectDevice([firstRun: true])

        then:
        thrown(MissingMethodException)
        ifaceCalls('socketConnect').size() == 1

        where:
        path << PATHS
    }

    @Unroll
    def "a failed connect still marks the device off before throwing: #path"() {
        given:
        def d = load(path, MagicHome.DEFAULTS)
        state.lastConnectionAttempt = clock
        InterfaceUtilsFail.arm()

        when:
        d.connectDevice([firstRun: true])

        then:
        thrown(MissingMethodException)
        sent('switch') == 'off'

        cleanup:
        InterfaceUtilsFail.disarm()

        where:
        path << PATHS
    }

    @Unroll
    def "refresh, poll, updated, initialize and socketStatus run (and reconnect) up to the typo: #path"() {
        given:
        def d = load(path, MagicHome.DEFAULTS)
        state.noResponse = 5
        state.lastConnectionAttempt = clock

        when:
        d.socketStatus('x')
        try { d.refresh() } catch (MissingMethodException ignored) { }
        try { d.poll() } catch (MissingMethodException ignored) { }
        try { d.updated() } catch (MissingMethodException ignored) { }
        try { d.initialize() } catch (MissingMethodException ignored) { }

        then:
        ifaceCalls('sendSocketMessage').size() >= 1

        where:
        path << PATHS
    }

    @Unroll
    def "a connect attempt that comes too soon is skipped (and then throws): #path"() {
        given:
        def d = load(path, MagicHome.DEFAULTS)
        state.lastConnectionAttempt = clock - 100

        when:
        d.connectDevice([firstRun: false])

        then:
        thrown(MissingMethodException)
        ifaceCalls('socketConnect').isEmpty()

        where:
        path << PATHS
    }
}
