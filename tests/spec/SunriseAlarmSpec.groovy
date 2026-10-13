import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification

interface Bulb {
    void setLevel(Object level, Object seconds)
    void setColor(Map color)
    void off()
    void setColorTemperature(Object ct, Object level, Object seconds)
}

class SunriseAlarmSpec extends Specification {
    AppExecutor api = Mock()
    Map state = [:]
    long clock = 1_000_000L

    def app(Map settings = [:]) {
        api.getLog() >> Stub(Log)
        api.getState() >> state
        api.now() >> { clock }
        new HubitatAppSandbox(new File('Apps/SunriseAlarm/SunriseAlarm.groovy')).run(
            api: api, userSettingValues: settings,
            validationFlags: [Flags.DontRunScript, Flags.AllowReadingNonInputSettings, Flags.DontValidateSubscriptions])
    }

    def "ramp starts dim red and ends bright at the final temperature"() {
        given:
        def s = app()

        expect:
        s.rampAt(0d, 4000) == [level: 1, hue: 2, sat: 100]
        s.rampAt(1d, 4000) == [level: 100, ct: 4000]
        s.rampAt(0.5d, 4000).level == 13   // cubic: 100 * 0.125, not 25
        (0..40).collect { s.rampAt(it / 40d, 4000).level } == (0..40).collect { s.rampAt(it / 40d, 4000).level }.sort()
    }

    def "cron is wake time minus ramp, rolling back over midnight"() {
        expect:
        app().cronFor(0, 7 * 60, 20) == '0 40 6 ? * 2'      // Mon 06:40
        app().cronFor(0, 10, 20) == '0 50 23 ? * 1'         // Mon 00:10 -> Sun 23:50
    }

    def "each enabled day gets its own handler so schedules don't overwrite each other"() {
        when:
        app([enabled: true, rampMinutes: 20,
             on0: true, time0: '2026-10-12T07:00:00.000-0400',
             on1: true, time1: '2026-10-13T06:30:00.000-0400',
             on2: false, time2: '2026-10-14T06:30:00.000-0400']).initialize()

        then:
        1 * api.schedule('0 40 6 ? * 2', 'startRamp0')
        1 * api.schedule('0 10 6 ? * 3', 'startRamp1')
        0 * api.schedule(_, 'startRamp2')
    }

    def "disabled alarm schedules nothing"() {
        when:
        app([enabled: false, on0: true, time0: '2026-10-12T07:00:00.000-0400']).initialize()

        then:
        0 * api.schedule(*_)
    }

    def "step fades level then colour, then temperature, and finishes the ramp"() {
        given:
        def bulb = Mock(Bulb)
        def s = app([bulbs: [bulb], rampMinutes: 1, maxCt: 4000])

        when: 'first step is the red phase'
        s.startRamp()

        then:
        1 * bulb.setLevel(1, 15)
        1 * bulb.setColor([hue: 2, saturation: 100])
        1 * api.runIn(30, 'step')

        when: 'last step is full brightness at the final temperature'
        state.step = 2
        s.step()

        then:
        1 * bulb.setColorTemperature(4000, 100, 15)
        !state.running
    }

    def "switching the bulb off cancels a running ramp"() {
        given:
        state.running = true

        when:
        app().offHandler([device: 'bulb'])

        then:
        !state.running
        1 * api.unschedule('step')
    }

    def "a full 20 minute ramp is 41 commands over 40 reschedules, ends at 100%, and never dims"() {
        given:
        def levels = []
        def bulb = Mock(Bulb) {
            setLevel(_, _) >> { levels << it[0] }
            setColorTemperature(_, _, _) >> { levels << it[1] }
        }
        def reschedules = 0
        api.runIn(30, 'step') >> { reschedules++ }
        def s = app([bulbs: [bulb], rampMinutes: 20, maxCt: 4000])

        when:
        s.startRamp()
        40.times { s.step() }

        then:
        reschedules == 40
        levels.size() == 41
        levels == levels.sort(false)
        levels.first() == 1
        levels.last() == 100
        !state.running
    }

    def "stepping after the ramp ended does nothing"() {
        given:
        def bulb = Mock(Bulb)

        when:
        app([bulbs: [bulb]]).step()

        then:
        0 * bulb._
    }

    def "every weekday handler starts the ramp"() {
        given:
        def bulb = Mock(Bulb)
        def s = app([bulbs: [bulb], rampMinutes: 20])

        when:
        s."startRamp$day"()

        then:
        state.running
        1 * bulb.setLevel(1, 15)

        where:
        day << (0..6)
    }

    def "apply-to-all copies the time and enables every day"() {
        given:
        def fakeApp = Mock(me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper)
        api.getApp() >> fakeApp

        when:
        app([allTime: '2026-10-12T07:00:00.000-0400']).appButtonHandler('applyAll')

        then:
        7 * fakeApp.updateSetting({ it ==~ /on\d/ }, [type: 'bool', value: true])
        7 * fakeApp.updateSetting({ it ==~ /time\d/ }, [type: 'time', value: '2026-10-12T07:00:00.000-0400'])
    }

    def "test button starts the ramp immediately"() {
        given:
        def bulb = Mock(Bulb)

        when:
        app([bulbs: [bulb], rampMinutes: 1]).appButtonHandler('testNow')

        then:
        state.running
        1 * bulb.setLevel(1, 15)
    }

    def "external changes are told apart from the finished ramp's own values"() {
        given:
        def s = app()

        expect:
        s.isExternalChange(name, value, 4000) == external

        where:
        name               | value  | external
        'switch'           | 'off'  | true
        'switch'           | 'on'   | false
        'level'            | '100'  | false
        'level'            | '60'   | true
        'colorTemperature' | '4000' | false
        'colorTemperature' | '3900' | false
        'colorTemperature' | '3000' | true
    }

    def "finishing the ramp arms auto-off and a new ramp disarms it"() {
        given:
        def s = app([bulbs: [Mock(Bulb)], rampMinutes: 1])

        when:
        s.startRamp()

        then:
        !state.untouched

        when:
        state.step = 2
        s.step()

        then:
        state.untouched
        state.doneAt == clock
    }

    def "auto-off switches the bulb off only if untouched"() {
        given:
        def bulb = Mock(Bulb)
        state.untouched = untouched

        when:
        app([bulbs: [bulb]]).autoOff()

        then:
        offs * bulb.off()

        where:
        untouched | offs
        true      | 1
        false     | 0
    }

    def "an external change after the grace period disarms auto-off, one inside it does not"() {
        given:
        state.untouched = true
        state.doneAt = doneAt

        when:
        app().changeHandler([name: 'level', value: '50', device: 'bulb'])

        then:
        state.untouched == stillArmed

        where:
        doneAt       | stillArmed
        1_000_000L   | true
        880_000L     | false
    }

    def "auto-off is scheduled daily when a time is set"() {
        when:
        app([enabled: true, autoOffTime: '2026-10-12T08:30:00.000-0400']).initialize()

        then:
        1 * api.schedule('0 30 8 ? * *', 'autoOff')
    }
}
