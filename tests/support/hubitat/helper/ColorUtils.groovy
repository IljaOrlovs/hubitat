package hubitat.helper

import java.awt.Color

class ColorUtils {
    static List hsvToRGB(List hsv) {
        int c = Color.HSBtoRGB((float) (hsv[0] / 100), (float) (hsv[1] / 100), (float) (hsv[2] / 100))
        [(c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF]
    }
    static List rgbToHSV(List rgb) {
        float[] h = Color.RGBtoHSB(rgb[0] as int, rgb[1] as int, rgb[2] as int, null)
        [h[0] * 100, h[1] * 100, h[2] * 100]
    }
    static String rgbToHEX(List rgb) { String.format('#%02X%02X%02X', rgb[0] as int, rgb[1] as int, rgb[2] as int) }
    static List hexToRGB(String hex) { [1, 3, 5].collect { Integer.parseInt(hex.replace('#', '').substring(it - 1, it + 1), 16) } }
    static List colorTempToRGB(int ct) { [255, 255, 255] }
}
