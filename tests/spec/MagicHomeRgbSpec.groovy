import spock.lang.Unroll

class MagicHomeRgbSpec extends DriverSpec {
    static final String PATH = 'Drivers/MagicHome/MagicHomeControllerRgb.groovy'
    static final Map DEFAULTS = [deviceIP: '10.0.0.5', devicePort: 5577, logDebug: true, logDescriptionText: true, turnOffWhenDisconnected: true,
                                 reconnectPings: 3, enablePreStaging: false, enableHueInDegrees: false, refreshTime: 10, recoverHueSaturation: true,
                                 neutralWhite: 4000, cwHue: 55, cwSaturationLowPoint: 0, cwSaturationHighPoint: 50,
                                 wwHue: 7.6, wwSaturationLowPoint: 0, wwSaturationHighPoint: 80]

    def "on and off send the power packets"() {
        given:
        def d = load(PATH, DEFAULTS)

        when:
        d.on(); d.off()

        then:
        ifaceCalls('sendSocketMessage')*.getAt(1) == ['7123 0FA3'.replace(' ', ''), '7124 0FA4'.replace(' ', '')]
        events*.value.containsAll(['on', 'off'])
    }
}
