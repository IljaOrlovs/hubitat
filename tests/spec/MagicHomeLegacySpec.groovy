import me.biocomp.hubitat_ci.api.helper.InterfaceUtils
import spock.lang.PendingFeature
import spock.lang.Unroll

/** The RGBW bulb for pre-2.1 firmware: its own getters, embedded colour maths, keep-alive loop and preset handling. */
class MagicHomeLegacySpec extends DriverSpec {
    static final String PATH = 'Drivers/MagicHome/MagicHomeBulbRgbwLegacyFirmware.groovy'
    static final Map CFG = MagicHome.DEFAULTS + [powerOnBrightnessChange: true]

    def driver(Map extra = [:]) {
        def d = load(PATH, CFG + extra)
        attrs += [switch: 'on', hue: 30, saturation: 40, level: 50, colorTemperature: 3500]
        d
    }

    @Unroll
    def "get#attr seeds a missing attribute with 100 and returns the stored one otherwise"() {
        given:
        def d = driver()

        when:
        attrs.remove(attr)
        def seeded = d."get${attr.capitalize()}"()
        attrs[attr] = 42
        def stored = d."get${attr.capitalize()}"()

        then:
        seeded == 100
        stored == 42

        where:
        attr << ['hue', 'saturation', 'level']
    }

    def "getColorTemperature seeds 2700 when unset"() {
        given:
        def d = driver()
        attrs.remove('colorTemperature')

        when:
        d.getColorTemperature()

        then:
        sent('colorTemperature') == 2700
    }

    def "setHue/Saturation/Level with transmit=false only publish the clamped value"() {
        given:
        def d = driver()

        expect:
        d.setHue(150, false) == 100
        d.setSaturation(-4, false) == 0
        d.setLevel(55, false) == 55
        !InterfaceUtils.calls
    }

    def "setColor sends a white-LED frame when hue is 100, an RGB frame otherwise"() {
        given:
        def d = driver()

        when:
        d.setColor([hue: 100, saturation: 0, level: 50])
        d.setColor([hue: 0, saturation: 100, level: 100])

        then:
        ifaceCalls('sendSocketMessage')*.getAt(1).findAll { it.startsWith('56') }.size() == 2
        sent('currentPreset') == 0
    }

    @Unroll
    def "setColorTemperature(#temp) goes through the #side-white branch"() {
        given:
        def d = driver()

        when:
        d.setColorTemperature(temp)

        then:
        sent('colorTemperature') == temp
        !ifaceCalls('sendSocketMessage').isEmpty()

        where:
        temp | side
        2700 | 'warm'
        6500 | 'cold'
    }

    def "presets: sendPreset(true) transmits; sendPreset(false) restores the colour"() {
        given:
        def d = driver()

        when:
        d.sendPreset(true, 3, 40)
        d.sendPreset(false)
        d.presetRedFade(10)

        then:
        ifaceCalls('sendSocketMessage')*.getAt(1).any { it.startsWith('61') }
        sent('currentPreset') != null
    }

    def "sendPreset clamps the preset to 1-20"() {
        given:
        def d = driver()

        when:
        d.sendPreset(true, 99, 50)

        then:
        sent('currentPreset') == 20 + 36
    }

    @Unroll
    def "powerOnWithChanges(append=#append) with powerOnBrightnessChange=#pob"() {
        given:
        def d = driver(powerOnBrightnessChange: pob)

        expect:
        d.powerOnWithChanges(true) == (pob ? [0x71, 0x23, 0x0F, 0xA3] : [])

        where:
        append | pob
        true   | true
        true   | false
    }

    def "normalizePercent clamps in either bound order and defaults null to the upper bound"() {
        given:
        def d = driver()

        expect:
        d.normalizePercent(null) == 100
        d.normalizePercent(5, 20, 10) == 10
        d.normalizePercent(30, 20, 10) == 20
        d.normalizePercent(15, 20, 10) == 15
    }

    def "checkIfInMap returns the parameter, else the current value, seeding when absent"() {
        given:
        def d = driver()

        expect:
        d.checkIfInMap(null, 'hue') == 30
        d.checkIfInMap(42, 'hue') == 42
    }

    def "hsvToRGB wraps the maximum hue to red and handles all six sextants"() {
        given:
        def d = driver()

        expect:
        [[0, 100, 100, 'low'], [100, 100, 100, 'low'], [10, 100, 100, 'low'], [25, 100, 100, 'low'], [40, 100, 100, 'low'],
         [55, 100, 100, 'low'], [70, 100, 100, 'low'], [90, 100, 100, 'low']].collect { h, s, v, r -> d.hsvToRGB(h, s, v, r) }.every { it.red != null }
        d.hsvToRGB(100, 100, 100, 'low').red == 255
    }

    def "rgbToHSV returns hue for each dominant channel"() {
        given:
        def d = driver()

        expect:
        Math.abs(d.rgbToHSV(255, 0, 0, 'high').hue - 0) < 1
        Math.abs(d.rgbToHSV(0, 255, 0, 'high').hue - 120) < 1
        Math.abs(d.rgbToHSV(0, 0, 255, 'high').hue - 240) < 1
        d.rgbToHSV(0, 0, 0, 'low').saturation == 0
        d.rgbToHSV(128, 0, 128, 'low').hue > 0     // negative hue wrapped into 0-100
    }

    def "initialize connects the socket and schedules the keep-alive; keepAlive polls and reschedules"() {
        given:
        def d = driver()

        when:
        d.initialize()
        d.keepAlive()

        then:
        ifaceCalls('socketConnect').size() == 1
        1 * api.runIn(20, _)
        1 * api.runIn(150, _)
        ifaceCalls('sendSocketMessage').size() == 1
    }

    def "refresh, poll, updated, socketStatus and telnetStatus"() {
        given:
        def d = driver()

        when:
        d.refresh(); d.poll(); d.updated()
        d.socketStatus('send error: Broken pipe (Write failed)')
        d.socketStatus('other')
        d.telnetStatus('x')

        then:
        ifaceCalls('sendSocketMessage').size() == 2
        ifaceCalls('socketConnect').size() == 1
        1 * api.runIn(10, _)
    }
}
