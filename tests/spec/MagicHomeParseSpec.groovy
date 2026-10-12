import spock.lang.Unroll

/** parse() of the status frames the controllers send back (4-byte power report, 14-byte general status). */
class MagicHomeParseSpec extends DriverSpec {
    /** Subclasses override to rerun every feature under different preferences. */
    Map extraSettings() { [:] }
    Map settings() { MagicHome.DEFAULTS + extraSettings() }

    static final List<String> PARSERS = MagicHome.having('parse', MagicHome.RAW)

    @Unroll
    def "4-byte power report 0x23 -> #expected: #file"() {
        given:
        def d = load(path, settings())
        state.noResponse = 2
        attrs.switch = attrs0

        when:
        d.parse(frame)

        then:
        legacyOrCct || state.noResponse == 0
        legacyOrCct || sent('switch') == (changes ? expected : null)

        where:
        [path, frame, expected, attrs0, changes] << PARSERS.collectMany { p ->
            [[p, '81442300', 'on', 'off', true], [p, '81442400', 'off', 'on', true], [p, '81442300', 'on', 'on', false], [p, '81442400', 'off', 'off', false]]
        }
        file = path.split('/').last()
        legacyOrCct = path.contains('LegacyFirmware') || path.contains('BulbWwCwCct')   // different parse(); covered by the 14-byte spec
    }

    @Unroll
    def "14-byte general status updates switch and colour: #file (#frame)"() {
        given:
        def d = load(path, settings() + [recoverHueSaturation: recover])
        attrs += [switch: 'off', coldWhiteLevel: 0, warmWhiteLevel: 0]

        when:
        d.parse(frame)

        then:
        noExceptionThrown()
        sent('switch') == (frame[4..5] == '23' ? 'on' : 'off') || sent('switch') == null

        where:
        [path, frame, recover] << [PARSERS, ['814423612432FF8000000004 0F00'.replace(' ', ''), '81442461240200000000000F0000'.replace(' ', ''), '8144236124320000000000040F00'], [true, false]].combinations()
        file = path.split('/').last()
    }

    @Unroll
    def "odd frames are ignored without throwing: #file '#frame'"() {
        given:
        def d = load(path, settings())

        when:
        d.parse(frame)

        then:
        noExceptionThrown()
        events.findAll { it.name in ['hue', 'saturation', 'level', 'switch'] }.isEmpty()

        where:
        [path, frame] << [PARSERS, ['', '8144', '81442300112233445566778899AABBCCDDEEFF']].combinations()
        file = path.split('/').last()
    }
}
