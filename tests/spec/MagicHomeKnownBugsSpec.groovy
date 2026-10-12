import spock.lang.PendingFeature
import spock.lang.Unroll

/**
 * Bugs found while writing these specs, left unfixed on purpose (no functional changes yet). Each test states the CORRECT behaviour
 * and is @PendingFeature: it is reported as "expected to fail" today and turns red the day the bug is fixed, at which point
 * delete the annotation (and move the driver into the shared specs it is currently excluded from).
 */
class MagicHomeKnownBugsSpec extends DriverSpec {
    def "WW/CW bulb does not re-send on when already on"() {
        given:
        def d = load('Drivers/MagicHome/MagicHomeBulbWwCwCct.groovy', MagicHome.DEFAULTS)
        attrs.switch = 'on'

        when:
        d.powerOnWithChanges()

        then:
        events.findAll { it.name == 'switch' }.isEmpty()
    }

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
