import spock.lang.PendingFeature
import spock.lang.Unroll

/** The WS28xx pixel-strip controller: raw-socket transport, effects, colour temperature, custom effects. */
class MagicHomeWs28xxSpec extends DriverSpec {
    static final String PATH = 'Drivers/MagicHome/MagicHomeWs28xx.groovy'

    def driver(Map extra = [:]) {
        def d = load(PATH, MagicHome.DEFAULTS + extra)
        attrs += [switch: 'on', colorMode: 'RGB', hue: 30, saturation: 40, level: 50, colorTemperature: 3500, effect: 5, effectSpeed: 60]
        state.lastConnectionAttempt = clock
        d
    }

    def "on and off send the power frames over the raw socket"() {
        given:
        def d = driver()

        when:
        d.on(); d.off()

        then:
        1 * raw.sendMessage('71230FA3')
        1 * raw.sendMessage('71240FA4')
    }

    @Unroll
    def "powerOnWithChanges: pre-staging=#pre switch=#sw -> sends on=#sends"() {
        given:
        def d = driver(enablePreStaging: pre)
        attrs.switch = sw

        when:
        d.powerOnWithChanges()

        then:
        (sends ? 1 : 0) * raw.sendMessage('71230FA3')

        where:
        pre   | sw    || sends
        false | 'off' || true
        false | 'on'  || false
        true  | 'off' || false
    }

    def "setColor with every component sends an RGB frame and selects RGB mode"() {
        given:
        def d = driver()

        when:
        d.setColor([hue: 0, saturation: 100, level: 100])

        then:
        1 * raw.sendMessage({ it.startsWith('31FF0000') })
        sent('colorMode') == 'RGB'
    }

    def "setColor from a colour temperature selects CT mode"() {
        given:
        def d = driver()

        when:
        d.setColor([hue: 10, saturation: 20, level: 30, setFromCT: true])

        then:
        sent('colorMode') == 'CT'
    }

    @Unroll
    def "setEffect(#arg) sends an effect frame and publishes effect + mode"() {
        given:
        def d = driver()

        when:
        d.setEffect(arg)

        then:
        1 * raw.sendMessage({ it.startsWith('61') })
        sent('colorMode') == 'EFFECTS'
        sent('effect') == expectedEffect

        where:
        arg    || expectedEffect
        7      || 7
        7G     || 7
        0      || 1      // clamped to the first effect
        1000   || 300    // clamped to the last effect
    }

    def "setEffect by name looks the effect up; unknown names are ignored"() {
        given:
        def d = driver()
        def name = d.lightEffects[3]

        when:
        d.setEffect(name)
        d.setEffect('no such effect')

        then:
        1 * raw.sendMessage({ it.startsWith('61') })
        sent('effect') == 3
    }

    def "setPreviousEffect steps back by one"() {
        given:
        def d = driver()
        attrs.effect = 5

        when:
        d.setPreviousEffect()

        then:
        sent('effect') == 4
    }

    @Unroll
    def "setColorMode(#mode) -> colorMode #expected"() {
        given:
        def d = driver()

        when:
        d.setColorMode(mode)

        then:
        sent('colorMode') == expected

        where:
        mode      || expected
        'RGB'     || 'RGB'
        'CT'      || 'CT'
        'CCT'     || 'CT'
        'FX'      || 'EFFECTS'
        'EFFECTS' || 'EFFECTS'
        'bogus'   || null
    }

    def "custom effects: a wrong-length function is rejected, a 580-char one is stored"() {
        given:
        def d = driver()
        def good = 'AB' * 290

        when:
        d.customEffectAdd('mine', 'short')

        then:
        state.customEffects == [:]

        when:
        d.customEffectAdd('mine', good)

        then:
        state.customEffects.collectEntries { k, v -> [k.toString(), v.toString()] } == [mine: good]   // stored as GStrings
    }

    def "removing or running an unknown custom effect is harmless"() {
        given:
        def d = driver()

        when:
        d.customEffectRemove('nope')
        d.customEffectRun('nope')

        then:
        noExceptionThrown()
        0 * raw.sendMessage(_)
    }

    def "initialize connects the raw socket and refresh polls over it"() {
        given:
        def d = driver()
        state.noResponse = 0

        when:
        d.initialize()
        d.refresh()

        then:
        1 * raw.connect(_, '10.0.0.5', 5577)
        (1.._) * raw.sendMessage(_)
    }

    def "a refused raw-socket connect is survivable"() {
        given:
        def d = driver()
        raw.connect(*_) >> { throw new IOException('refused') }

        when:
        d.connectDevice([firstRun: true])

        then:
        noExceptionThrown()
    }

    @Unroll
    def "parse of the 4-byte power report (#frame) -> switch #expected"() {
        given:
        def d = driver()
        state.noResponse = 3

        when:
        d.parse(frame)

        then:
        state.noResponse == 0
        sent('switch') == expected

        where:
        frame      || expected
        '81442300' || 'on'
        '81442400' || 'off'
    }

    @Unroll
    def "parse of the 14-byte status with effect word #word -> #mode"() {
        given:
        def d = driver()

        when:
        d.parse("814423${word}20FF00000000000000".padRight(28, '0').take(28))

        then:
        sent('colorMode') == mode
        sent('effectSpeed') == 0x20
        sent('switch') == 'on'

        where:
        word   || mode
        '0061' || 'RGB'       // 97: static colour
        '0060' || 'EFFECTS'   // 96: custom effect
        '006E' || 'EFFECTS'   // preset effect 11
    }

    def "parse ignores frames of any other length"() {
        given:
        def d = driver()

        when:
        d.parse('8144')

        then:
        events.isEmpty()
    }

    @PendingFeature(reason = 'customEffectRun(name) reads state.customEffects."name".value, but the stored entry is a plain string, so it always reports "could not find effect"')
    def "a stored custom effect can be run by name"() {
        given:
        def d = driver()
        def fn = 'AB' * 290
        d.customEffectAdd('mine', fn)

        when:
        d.customEffectRun('mine')

        then:
        sent('effectName') == 'Custom'
        sent('effect') == 0
        1 * raw.sendMessage({ it.startsWith('AB') })

    }

    def "a stored custom effect can be removed"() {
        given:
        def d = driver()
        d.customEffectAdd('mine', 'AB' * 290)

        when:
        d.customEffectRemove('mine')

        then:
        !state.customEffects.containsKey('mine')
    }

    @PendingFeature(reason = 'customEffectRun(int)/customEffectRemove(int) index the effects Map with a number (effectNumber + 1), which is always null')
    def "custom effects by number address the stored effects"() {
        given:
        def d = driver()
        state.customEffects = [first: 'AB' * 290]

        when:
        d.customEffectRun(0)
        d.customEffectRemove(0)

        then:
        1 * raw.sendMessage({ it.startsWith('AB') })
        state.customEffects.isEmpty()
    }

    @PendingFeature(reason = 'the duplicate check looks up "${name}" (the device name) instead of effectName, so duplicates overwrite')
    def "adding a custom effect with an existing name is refused"() {
        given:
        def d = driver()
        def fn = 'AB' * 290
        d.customEffectAdd('mine', fn)

        when:
        d.customEffectAdd('mine', 'CD' * 290)

        then:
        state.customEffects.collectEntries { k, v -> [k.toString(), v.toString()] } == [mine: fn]
    }

    def "poll, updated and socketStatus"() {
        given:
        def d = driver()
        state.noResponse = 0

        when:
        d.poll()
        d.updated()
        d.socketStatus('closed')

        then:
        (1.._) * raw.sendMessage(_)
        (1.._) * raw.connect(_, '10.0.0.5', 5577)
    }

    // ---- bugs: the correct behaviour, pending a fix ----

    @PendingFeature(reason = "setColor's try/catch fallbacks never fire (a Map returns null, it does not throw), so partial colour updates NPE on toFloat()")
    @Unroll
    def "#setter(#value) sends a colour frame keeping the other components"() {
        given:
        def d = driver()

        when:
        d."$setter"(value)

        then:
        1 * raw.sendMessage({ it.startsWith('31') })

        where:
        setter          | value
        'setHue'        | 50
        'setSaturation' | 70
        'setLevel'      | 80
    }

    @PendingFeature(reason = 'setColorTemperature ignores its argument and reads an undefined colorTemperature variable')
    def "setColorTemperature(3000) publishes the temperature and switches to CT"() {
        given:
        def d = driver()

        when:
        d.setColorTemperature(3000, 50)

        then:
        sent('colorTemperature') == 3000
        sent('colorMode') == 'CT'
    }

    @PendingFeature(reason = 'setEffectSpeed calls setEffect(null, speed), for which no overload exists')
    def "setEffectSpeed changes only the speed, clamped to 1-100"() {
        given:
        def d = driver()

        when:
        d.setEffectSpeed(500)

        then:
        sent('effectSpeed') == 100
    }

    @PendingFeature(reason = 'setNextEffect compares against lightEffects.length (null for a Map), so it always wraps to effect 1')
    def "setNextEffect advances by one"() {
        given:
        def d = driver()
        attrs.effect = 5

        when:
        d.setNextEffect()

        then:
        sent('effect') == 6
    }

    @PendingFeature(reason = 'setPreviousEffect from effect 1 wraps to lightEffects.length (null) and then fails')
    def "setPreviousEffect wraps from the first effect to the last"() {
        given:
        def d = driver()
        attrs.effect = 1

        when:
        d.setPreviousEffect()

        then:
        sent('effect') == 300
    }
}
