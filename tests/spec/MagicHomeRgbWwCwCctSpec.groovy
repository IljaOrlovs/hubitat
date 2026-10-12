import spock.lang.PendingFeature

/** RGB + warm/cold-white bulb: the status frame carries white levels, which decide between RGB and colour-temperature mode. */
class MagicHomeRgbWwCwCctSpec extends DriverSpec {
    static final String PATH = 'Drivers/MagicHome/MagicHomeBulbRgbWwCwCct.groovy'

    def driver() {
        def d = load(PATH, MagicHome.DEFAULTS)
        attrs += [switch: 'on', coldWhiteLevel: 0, warmWhiteLevel: 0, level: 50]
        d
    }

    @PendingFeature(reason = 'the CT branch evaluates device.currentValue > 4 (a method, not a call), which throws MissingPropertyException, so any frame with white LEDs lit fails to parse')
    def "a frame with white-LED levels switches to CT mode and publishes both levels and the temperature"() {
        given:
        def d = driver()

        when:
        d.parse('8144236124320000008000400F00')   // warm = 0x80 -> ~50%, cold = 0x40 -> ~25%

        then:
        sent('colorMode') == 'CT'
        Math.abs(sent('warmWhiteLevel') - 50.2) < 0.1
        Math.abs(sent('coldWhiteLevel') - 25.1) < 0.1
        sent('colorTemperature') != null
    }

    def "a frame without white levels is RGB: hue/saturation are only trusted above 5% brightness"() {
        given:
        def d = driver()

        when:
        d.parse(frame)

        then:
        sent('colorMode') == 'RGB'
        (sent('hue') != null) == trusted

        where:
        frame                          || trusted
        '8144236124320000000000000F00' || false    // black: hue/saturation are not recoverable
        '8144236124320000FF0000000F00' || true     // full-brightness blue
    }
}
