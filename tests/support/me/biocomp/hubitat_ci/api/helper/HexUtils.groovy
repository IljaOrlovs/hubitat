package me.biocomp.hubitat_ci.api.helper

class HexUtils {
    static String byteArrayToHexString(b) { b.collect { it as int }.collect { String.format('%02X', it & 0xFF) }.join() }
    static byte[] hexStringToByteArray(String s) { (0..<s.length().intdiv(2)).collect { Integer.parseInt(s.substring(it * 2, it * 2 + 2), 16) as byte } as byte[] }
    static int[] hexStringToIntArray(String s) { (0..<s.length().intdiv(2)).collect { Integer.parseInt(s.substring(it * 2, it * 2 + 2), 16) } as int[] }
    static String intArrayToHexString(a) { a.collect { String.format('%02X', it & 0xFF) }.join() }
    static int hexStringToInt(String s) { Integer.parseInt(s, 16) }
    static String integerToHexString(BigDecimal v, Integer bytes) { String.format("%0${bytes * 2}X", v.toInteger()) }
}
