import me.biocomp.hubitat_ci.api.helper.InterfaceUtils
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.InterfaceHelper
import me.biocomp.hubitat_ci.api.common_api.RawSocket
import me.biocomp.hubitat_ci.api.common_api.WebSocket
import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification

/**
 * Base for driver specs. `load(path, settings)` returns the driver script with the Hubitat API mocked:
 * `state` and `attrs` (current attribute values) are plain maps, every sendEvent lands in `events`,
 * `now()` returns `clock`, and hubitat.helper.InterfaceUtils calls are recorded in InterfaceUtils.calls.
 */
/** hubitat_ci 0.17's DeviceExecutor has no `settings`; scripts still read it, so expose it on the mock. */
interface DeviceApi extends DeviceExecutor { Map getSettings() }

abstract class DriverSpec extends Specification {
    DeviceApi api = Mock()
    RawSocket raw = Mock()
    WebSocket ws = Mock()
    Map state = [:]
    Map attrs = [:]
    List events = []
    long clock = 1_000_000L

    def setup() { InterfaceUtils.calls.clear() }

    def load(String path, Map settings = [:], Map extra = [:]) {
        api.getLog() >> Stub(Log)
        api.getInterfaces() >> Stub(InterfaceHelper) { getRawSocket() >> raw; getWebSocket() >> ws }
        api.getState() >> state
        api.getDevice() >> Stub(DeviceWrapper) { currentValue(_) >> { a -> attrs[a[0]] } }
        api.sendEvent(_) >> { Map e -> events << e; attrs[e.name] = e.value }
        api.now() >> { clock }
        def file = new File(path)
        new HubitatDeviceSandbox(file).run([api: api, userSettingValues: [settings: settings], 
                                            validationFlags: Scripts.flagsFor(file) + [Flags.AllowReadingNonInputSettings]] + extra)
    }

    // hubitat_ci device scripts have no `settings` object: a bare `settings` resolves like any input name,
    // so we hand it a user-setting called "settings" that holds the whole map (needs AllowReadingNonInputSettings).

    /** Last value sent for an event name, or null. */
    def sent(String name) { events.reverse().find { it.name == name }?.value }

    List ifaceCalls(String method) { InterfaceUtils.calls.findAll { it[0] == method }*.getAt(1) }
}
