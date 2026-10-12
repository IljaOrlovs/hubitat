// Deliberately small: bugs and dead code only. Style is left to .editorconfig.
ruleset {
    description 'Hubitat bug-hunting rules'
    ruleset('rulesets/basic.xml') {
        exclude 'EmptyMethod'      // installed()/updated() stubs are idiomatic
        exclude 'ExplicitGarbageCollection'
    }
    ruleset('rulesets/unused.xml') {
        exclude 'UnusedMethodParameter' // Hubitat callbacks need fixed signatures
        exclude 'UnusedPrivateMethodParameter'
        exclude 'UnusedPrivateMethod' // string-named callbacks (runIn/subscribe) look unused
    }
    ruleset('rulesets/imports.xml')
    DuplicateMapKey
    DuplicateCaseStatement
    ConstantIfExpression
    ReturnNullFromCatchBlock
}
