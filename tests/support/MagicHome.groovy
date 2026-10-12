/** Shared fixtures for the MagicHome driver specs. */
class MagicHome {
    static final List<String> DRIVERS = Scripts.scripts('Drivers/MagicHome')*.path

    /** Union of every preference default used across the family; extra keys are harmless to drivers that lack them. */
    static final Map DEFAULTS = [deviceIP: '10.0.0.5', devicePort: 5577, logDebug: true, logDescriptionText: true,
                                 turnOffWhenDisconnected: true, reconnectPings: 3, enablePreStaging: false, enableHueInDegrees: false,
                                 refreshTime: 10, recoverHueSaturation: true, neutralWhite: 4000, cwHue: 55, cwSaturationLowPoint: 0,
                                 cwSaturationHighPoint: 50, wwHue: 7.6, wwSaturationLowPoint: 0, wwSaturationHighPoint: 80,
                                 deviceCWTemperature: 6500, deviceWWTemperature: 2700, colorTemperatureMin: 2700, colorTemperatureMax: 6500,
                                 enableRGBWColors: false, whiteLevel: 100, dimmableLevel: 100, bulbType: 'rgbw']

    /** Paths in the family that define `method`, minus drivers named in `except` (file-name suffix after "MagicHome"). */
    static List<String> having(String method, List<String> except = []) {
        Scripts.defining('Drivers/MagicHome', method).findAll { p -> !except.any { p.endsWith("MagicHome${it}.groovy") } }
    }

    /** Drivers that talk over interfaces.rawSocket instead of hubitat.helper.InterfaceUtils. */
    static final List<String> RAW = ['Ws28xx']
    /** Drivers whose connectDevice calls the misspelt logdebug() and so throw MissingMethodException (see MagicHomeLogTypoSpec). */
    static final List<String> LOG_TYPO = ['BulbRgbWwCwCct', 'BulbWwCwCct']
}
