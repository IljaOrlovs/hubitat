import spock.lang.Unroll

// parse() itself can't run under hubitat_ci (the stubbed HexUtils/log.trace don't take what it passes), so test the guard.
class MinecraftServerSpec extends DriverSpec {
    @Unroll
    def "asInt(#text) is #expected"() {
        expect:
        load('Drivers/MinecraftServer/MinecraftServer.groovy').asInt(text) == expected

        where:
        text   | expected
        '12'   | 12
        ' 3 '  | 3
        'abc'  | 0
        ''     | 0
        null   | 0
    }
}
