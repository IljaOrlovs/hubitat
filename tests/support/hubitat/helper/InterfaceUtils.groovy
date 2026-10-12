package hubitat.helper

/** Newer Hubitat helper that hubitat_ci 0.17 predates; tests mock it where they care. */
class InterfaceUtils {
    static void sendWebSocketMessage(device, String message) {}
    static void webSocketConnect(device, String url, Map options = [:]) {}
    static void webSocketClose(device) {}
}
