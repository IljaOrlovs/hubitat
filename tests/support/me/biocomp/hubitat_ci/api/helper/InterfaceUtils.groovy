package me.biocomp.hubitat_ci.api.helper

/** Newer Hubitat helper that hubitat_ci 0.17 predates. Records every call in `calls` as [method, args]; `failOn` makes that method throw. */
class InterfaceUtils {
    static List calls = []
    static String failOn = null
    static $static_methodMissing(String name, args) {
        calls << [name, args as List]
        if (name == failOn) { throw new IOException("simulated ${name} failure") }
        null
    }
}
