import spock.lang.Specification
import spock.lang.Unroll

class ColorMethodsSpec extends Specification {
    def hsv = new GroovyShell().parse(new File('Methods/hsvToRGB.groovy'))
    def rgb = new GroovyShell().parse(new File('Methods/rgbToHSV.groovy'))

    @Unroll
    def "hsvToRGB(#h, #s, #v, #res) -> #r,#g,#b"() {
        when:
        def c = hsv.hsvToRGB(h, s, v, res)

        then:
        Math.round(c.red) == r && Math.round(c.green) == g && Math.round(c.blue) == b

        where:
        h   | s   | v   | res    || r   | g   | b
        0   | 100 | 100 | 'low'  || 255 | 0   | 0
        100 | 100 | 100 | 'low'  || 255 | 0   | 0   // max hue wraps to red, not black
        360 | 100 | 100 | 'high' || 255 | 0   | 0
        120 | 100 | 100 | 'high' || 0   | 255 | 0
        240 | 100 | 100 | 'high' || 0   | 0   | 255
        0   | 0   | 100 | 'low'  || 255 | 255 | 255
        50  | 100 | 0   | 'low'  || 0   | 0   | 0
        -5  | 100 | 100 | 'low'  || 255 | 0   | 0   // clamps below range
        500 | 100 | 100 | 'high' || 255 | 0   | 0   // clamps above range
    }

    @Unroll
    def "rgbToHSV(#r,#g,#b) -> hue #h"() {
        when:
        def c = rgb.rgbToHSV(r, g, b, 'high')

        then:
        Math.abs(c.hue - h) < 1 && Math.abs(c.saturation - s) < 1 && Math.abs(c.value - v) < 1

        where:
        r   | g   | b   || h   | s   | v
        255 | 0   | 0   || 0   | 100 | 100
        0   | 255 | 0   || 120 | 100 | 100
        0   | 0   | 255 || 240 | 100 | 100
        10  | 200 | 90  || 145 | 95  | 78   // regression: float vs BigDecimal compare left hue at 0
        128 | 128 | 128 || 0   | 0   | 50
        0   | 0   | 0   || 0   | 0   | 0
    }

    def "low resolution scales hue to 0-100"() {
        expect:
        Math.abs(rgb.rgbToHSV(0, 0, 255, 'low').hue - 66.67) < 0.5
    }

    def "hsv -> rgb -> hsv round trip"() {
        when:
        def c = rgb.rgbToHSV(10, 200, 90, 'high')
        def back = hsv.hsvToRGB(c.hue, c.saturation, c.value, 'high')

        then:
        [back.red, back.green, back.blue].collect { Math.round(it) } == [10, 200, 90]
    }
}
