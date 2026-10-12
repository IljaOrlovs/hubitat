/** The same MagicHome specs again with logging off, hue in degrees, no recover-hue and no switch-off on disconnect, to cover the other branches. */
class MagicHomeQuietSpecs {
    static final Map QUIET = [logDebug: false, logDescriptionText: false, turnOffWhenDisconnected: false, enableHueInDegrees: true, recoverHueSaturation: false]
}

class MagicHomeQuietCommonSpec extends MagicHomeCommonSpec { Map extraSettings() { MagicHomeQuietSpecs.QUIET } }
class MagicHomeQuietCommandsSpec extends MagicHomeCommandsSpec { Map extraSettings() { MagicHomeQuietSpecs.QUIET } }
class MagicHomeQuietParseSpec extends MagicHomeParseSpec { Map extraSettings() { MagicHomeQuietSpecs.QUIET } }
