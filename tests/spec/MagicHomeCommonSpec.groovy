import spock.lang.Unroll

/** Behaviour shared by the MagicHome drivers: checksum, clamping, connection handling and the socket send path. */
class MagicHomeCommonSpec extends DriverSpec {
    /** Subclasses override to rerun every feature under different preferences. */
    Map extraSettings() { [:] }
    Map settings() { MagicHome.DEFAULTS + extraSettings() }

    @Unroll
    def "calculateChecksum sums bytes modulo 256 and appendChecksum appends it: #path"() {
        given:
        def d = load(path, settings())

        expect:
        d.calculateChecksum([0x31, 0xFF, 0x80]) == 0xB0
        d.calculateChecksum([0xFF, 0xFF, 0x02]) == 0x00
        d.calculateChecksum([]) == 0
        d.appendChecksum([0x71, 0x23, 0x0F]) == [0x71, 0x23, 0x0F, 0xA3]

        where:
        path << Scripts.defining('Drivers/MagicHome', 'calculateChecksum')
    }

    @Unroll
    def "limit clamps to the bounds in either order and defaults null to the upper bound: #path"() {
        given:
        def d = load(path, settings())

        expect:
        d.limit(150) == 100
        d.limit(-5) == 0
        d.limit(50) == 50
        d.limit(null) == 100
        d.limit(7, 10, 20) == 10
        d.limit(25, 10, 20) == 20
        d.limit(5, 20, 10) == 10   // reversed bounds
        d.limit(25, 20, 10) == 20
        d.limit(15, 20, 10) == 15

        where:
        path << Scripts.defining('Drivers/MagicHome', 'limit')
    }

    @Unroll
    def "sendCommand hex-encodes the frame onto the socket: #path"() {
        given:
        def d = load(path, settings())

        when:
        d.sendCommand([0x71, 0x23, 0x0F, 0xA3] as byte[])

        then:
        ifaceCalls('sendSocketMessage').collect { it[1] } == ['71230FA3']

        where:
        path << MagicHome.having('sendCommand', MagicHome.RAW)
    }

    @Unroll
    def "installed seeds a switched-off device and zeroes the failed-ping counter: #path"() {
        given:
        state.noResponse = 5
        def d = load(path, settings())

        when:
        d.installed()

        then:
        sent('switch') == 'off'
        state.noResponse == 0

        where:
        path << MagicHome.having('installed', ['SmartPlug', 'Ws28xx', 'ControllerRgbw', 'BulbControllerDimmable', 'BulbRgbWwCwCct'])
    }

    @Unroll
    def "initialize starts a first-run connection and clears legacy state keys: #path"() {
        given:
        state.initializeLoopRunning = true
        state.oldvariablename = 1
        def d = load(path, settings())

        when:
        d.initialize()

        then:
        !state.containsKey('initializeLoopRunning')
        !state.containsKey('oldvariablename')
        state.lastConnectionAttempt == clock
        ifaceCalls('socketConnect').size() == 1
        '10.0.0.5' in ifaceCalls('socketConnect')[0]

        where:
        path << MagicHome.having('connectDevice', MagicHome.RAW + MagicHome.LOG_TYPO + ['SmartPlug'])
    }

    @Unroll
    def "connectDevice skips a retry that comes too soon after the last attempt: #path"() {
        given:
        def d = load(path, settings())
        state.lastConnectionAttempt = clock - 1000   // refreshTime 10s -> threshold 5000ms

        when:
        d.connectDevice([firstRun: false])

        then:
        ifaceCalls('socketConnect').isEmpty()
        state.lastConnectionAttempt == clock

        where:
        path << MagicHome.having('connectDevice', MagicHome.RAW + MagicHome.LOG_TYPO)
    }

    @Unroll
    def "connectDevice retries once enough time has passed: #path"() {
        given:
        def d = load(path, settings())
        state.lastConnectionAttempt = clock - 60_000

        when:
        d.connectDevice([firstRun: false])

        then:
        ifaceCalls('socketConnect').size() == 1
        state.noResponse == 0

        where:
        path << MagicHome.having('connectDevice', MagicHome.RAW + MagicHome.LOG_TYPO)
    }

    @Unroll
    def "a failed socket connect marks the device off only when configured to: #path"() {
        given:
        def d = load(path, settings())
        state.lastConnectionAttempt = clock
        InterfaceUtilsFail.arm()

        when:
        d.connectDevice([firstRun: true])

        then:
        (sent('switch') == 'off') == settings().turnOffWhenDisconnected
        state.noResponse == null

        cleanup:
        InterfaceUtilsFail.disarm()

        where:
        path << MagicHome.having('connectDevice', MagicHome.RAW + MagicHome.LOG_TYPO)
    }

    @Unroll
    def "refresh counts a missed response, polls the device, and reconnects after reconnectPings misses: #path"() {
        given:
        state.noResponse = misses
        def d = load(path, settings())
        state.lastConnectionAttempt = clock

        when:
        d.refresh()

        then:
        state.noResponse == (reconnects ? 0 : misses + 1)   // a reconnect resets the counter
        ifaceCalls('sendSocketMessage').size() == 1
        (ifaceCalls('socketConnect').size() == 1) == reconnects

        where:
        [path, misses] << [MagicHome.having('refresh', MagicHome.RAW + MagicHome.LOG_TYPO + ['BulbRgbwLegacyFirmware']), [0, 5]].combinations()
        reconnects = misses >= 3
    }

    @Unroll
    def "poll, updated and socketStatus delegate sensibly: #path"() {
        given:
        state.noResponse = 0
        def d = load(path, settings())
        state.lastConnectionAttempt = clock

        when:
        d.poll()
        d.updated()
        d.socketStatus('error')

        then:
        state.noResponse >= 0
        ifaceCalls('socketConnect').size() >= 1

        where:
        path << MagicHome.having('poll', MagicHome.RAW + MagicHome.LOG_TYPO + ['BulbRgbwLegacyFirmware'])
    }
}
