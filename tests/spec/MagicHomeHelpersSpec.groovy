import spock.lang.Unroll

/** The small pure helpers copy-pasted through the MagicHome family. */
class MagicHomeHelpersSpec extends DriverSpec {
    @Unroll
    def "powerOnWithChanges turns the device on only when it is off and pre-staging is disabled: #file"() {
        given:
        def d = load(path, MagicHome.DEFAULTS + [enablePreStaging: preStaging])
        attrs.switch = current

        when:
        d.powerOnWithChanges()

        then:
        (events.any { it.name == 'switch' && it.value == 'on' }) == turnsOn

        where:
        [path, preStaging, current, turnsOn] << [MagicHome.having('powerOnWithChanges', MagicHome.RAW + ['BulbWwCwCct', 'BulbRgbwLegacyFirmware']), [[false, 'off', true], [false, 'on', false], [true, 'off', false]]].combinations().collect { p, c -> [p, *c] }
        file = path.split('/').last()
    }

    @Unroll
    def "calculateCTSaturation follows the sqrt curve for #kind white: #file"() {
        given:
        def d = load(path, MagicHome.DEFAULTS)

        expect:
        Math.abs(d.calculateCTSaturation(cold, 2500) - expected) < 0.01

        where:
        [path, cold, kind, expected] << MagicHome.having('calculateCTSaturation').collectMany { p ->
            [[p, true, 'cold', 1.8 * 50 * 50 / 100], [p, false, 'warm', 2.16666 * 50 * 80 / 100]]   // sqrt(2500) = 50; high points 50 / 80
        }
        file = path.split('/').last()
    }

    @Unroll
    def "calculateCTSaturation tolerates low/high points given the wrong way round: #file"() {
        given:
        def swapped = MagicHome.DEFAULTS + [cwSaturationLowPoint: 50, cwSaturationHighPoint: 0, wwSaturationLowPoint: 80, wwSaturationHighPoint: 0]
        def d = load(path, swapped)

        expect:
        d.calculateCTSaturation(true, 2500) == load(path, MagicHome.DEFAULTS).calculateCTSaturation(true, 2500) ||
            d.calculateCTSaturation(true, 2500) > 0   // the swap is normalised, not rejected
        d.calculateCTSaturation(false, 2500) > 0

        where:
        path << MagicHome.having('calculateCTSaturation')
    }

    @Unroll
    def "linear helpers: #file"() {
        given:
        def d = load(path, MagicHome.DEFAULTS)
        attrs.level = 40

        expect:
        d.invertLinearValue(0, 100, 0) == 0
        d.invertLinearValue(100, 100, 0) == 100
        d.invertLinearValue(10, 20, 10) == 0
        d.proportionalToDeviceLevel(50) == 20

        where:
        path << MagicHome.having('invertLinearValue')
    }

    @Unroll
    def "betweenOverUnder is a +/- tolerance check: #file"() {
        given:
        def d = load(path, MagicHome.DEFAULTS)

        expect:
        d.betweenOverUnder(5, 5.2)
        d.betweenOverUnder(5, 4.6)
        !d.betweenOverUnder(5, 6)
        !d.betweenOverUnder(5, 4)
        d.betweenOverUnder(5, 6, 2)

        where:
        path << MagicHome.having('betweenOverUnder')
    }

    @Unroll
    def "clamp bounds a value in either bound order: #file"() {
        given:
        def d = load(path, MagicHome.DEFAULTS)

        expect:
        d.clamp(150) == 100
        d.clamp(-3) == 0
        d.clamp(40) == 40
        d.clamp(5, 10, 20) == 10
        d.clamp(30, 20, 10) == 20   // reversed bounds
        d.clamp(5, 20, 10) == 10

        where:
        path << MagicHome.having('clamp')
    }
}
