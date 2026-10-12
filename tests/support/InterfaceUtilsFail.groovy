import me.biocomp.hubitat_ci.api.helper.InterfaceUtils

/** Makes InterfaceUtils.socketConnect throw, to exercise connection-failure paths. */
class InterfaceUtilsFail {
    static void arm() { InterfaceUtils.failOn = 'socketConnect' }
    static void disarm() { InterfaceUtils.failOn = null }
}
