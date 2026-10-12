import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.InterfaceHelper
import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.api.common_api.WebSocket
import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification

class AccessDoorbellSpec extends Specification {
    DeviceExecutor api = Mock()
    WebSocket ws = Mock()

    def script(Map settings = [host: 'h', port: 12445, token: 't', pingInterval: 30]) {
        api.getLog() >> Stub(Log)
        api.getInterfaces() >> Stub(InterfaceHelper) { getWebSocket() >> ws }
        api.parseJson(_) >> { new groovy.json.JsonSlurper().parseText(it[0]) }
        new HubitatDeviceSandbox(new File('Drivers/Unifi/AccessDoorbell.groovy')).run(
            api: api, validationFlags: [Flags.DontRunScript, Flags.AllowReadingNonInputSettings, Flags.DontRequireParseMethodInDevice],
            userSettingValues: settings
        )
    }

    def "ringing the doorbell holds button 1 and shows presence"() {
        when:
        script().parse('{"event":"access.remote_view"}')

        then:
        1 * api.sendEvent([name: 'held', value: 1, isStateChange: true])
        1 * api.sendEvent([name: 'presence', value: 'present', isStateChange: true])
    }

    def "cancelling releases the button that was held"() {
        when:
        script().parse('{"event":"access.remote_view.change"}')

        then:
        1 * api.sendEvent([name: 'released', value: 1, isStateChange: true])
        1 * api.sendEvent([name: 'presence', value: 'not present', isStateChange: true])
    }

    def "physical button press holds, then auto-releases after 11s"() {
        when:
        def s = script()
        s.parse('{"event":"access.hw.door_bell"}')

        then:
        1 * api.sendEvent([name: 'held', value: 1, isStateChange: true])
        1 * api.runInMillis(11000, _, _)
    }

    def "the 8-character keepalive marks the device online"() {
        when:
        script().parse('12345678')

        then:
        1 * api.sendEvent([name: 'healthStatus', value: 'online'])
    }

    def "non-JSON and unknown messages are ignored without events"() {
        when:
        def s = script()
        s.parse('this is not json')
        s.parse('{"event":"something.else"}')

        then:
        0 * api.sendEvent(_)
    }

    def "initialize announces one button and opens the socket with the configured ping interval"() {
        when:
        script().initialize()

        then:
        1 * api.sendEvent([name: 'numberOfButtons', value: 1])
        1 * ws.connect({ it.pingInterval == 30 && it.headers.Authorization == 'Bearer t' }, 'wss://h:12445/api/v1/developer/devices/notifications')
    }

    def "ping interval defaults to 60"() {
        when:
        script([host: 'h', port: 1, token: 't']).requestNotifications()

        then:
        1 * ws.connect({ it.pingInterval == 60 }, _)
    }

    def "a failing connect is logged, not thrown"() {
        given:
        ws.connect(_, _) >> { throw new IOException('boom') }

        when:
        script().requestNotifications()

        then:
        noExceptionThrown()
    }

    def "updated closes the old socket before reconnecting"() {
        when:
        script().updated()

        then:
        1 * api.unschedule()
        1 * ws.close()
        1 * ws.connect(_, _)
    }

    def "opening the socket cancels pending reconnects and reports online"() {
        when:
        script().webSocketStatus('status: open')

        then:
        2 * api.unschedule(_)
        1 * api.sendEvent([name: 'healthStatus', value: 'online'])
    }

    def "closing the socket reports offline and schedules a reconnect"() {
        when:
        script().webSocketStatus('status: closed')

        then:
        1 * api.sendEvent([name: 'healthStatus', value: 'offline'])
        1 * api.runIn(60, _)
        1 * api.runIn(90, _)
    }

    def "poll reconnects only when not online"() {
        given:
        api.getDevice() >> Stub(DeviceWrapper) { currentValue('healthStatus') >> state }
        def s = script()

        when:
        s.poll()

        then:
        calls * ws.connect(_, _)

        where:
        state     || calls
        'offline' || 1
        'online'  || 0
    }

    def "push, hold and release forward the button number"() {
        when:
        def s = script()
        s.push(2); s.hold(3); s.release(4)

        then:
        1 * api.sendEvent([name: 'pushed', value: 2, isStateChange: true])
        1 * api.sendEvent([name: 'held', value: 3, isStateChange: true])
        1 * api.sendEvent([name: 'released', value: 4, isStateChange: true])
    }
}
