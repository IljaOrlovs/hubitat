import me.biocomp.hubitat_ci.validation.Flags

class Scripts {
    static final List<Flags> LEGACY = [
        Flags.DontRequireParseMethodInDevice, Flags.AllowMissingInstall, Flags.AllowReadingNonInputSettings,
        Flags.AllowWritingToSettings, Flags.AllowUnreachablePages, Flags.AllowTitleInPageCallingMethods,
        Flags.AllowMissingCommandMethod, Flags.DontRequireCapabilityImplementationMethods, Flags.AllowSectionsInDevicePreferences,
        Flags.AllowMissingDeviceInputNameOrType, Flags.AllowNullListOptions, Flags.AllowEmptyOptionValueStrings, Flags.AllowNullEnumInputOptions,
    ]

    /** Scripts that still trip a validator. Fix the script and delete its line; never add one without a reason. */
    // DontRunScript: still compiles and instantiates the script, but skips running definition()/preferences() through the validators
    static final List<Flags> RELAXED = [Flags.DontRunScript]
    static final Set<String> KNOWN = [
        // hubitat_ci 0.17 lags the platform (page install:, submitOnChange, MediaInputSource, 'STRING' types, command arg maps) or the script
        // genuinely breaks a rule (Heatit: preferences() outside metadata(); RootinTootin/MagicHome: nested preferences). Each is debt.
        'Apps/SaltAndPepper/SaltAndPepper.groovy',
        'Apps/SaltAndPepper/SaltAndPepperChild.groovy',
        'Apps/CircadianDaylight.groovy',
        'Apps/Utilities/PercentToColorTemperature/PercentToColorTemperatureChild.groovy',
        'Apps/Utilities/PercentToColorTemperature/PercentToColorTemperature.groovy',
        'Apps/SuperFan/SuperFanChild.groovy',
        'Apps/SimpleSprinklers/SimpleSprinklersChild.groovy',
        'Apps/Utilities/RootinTootinSelfRebootin.groovy',
        'Apps/MagicHome/MagicHomeManager.groovy',
        'Drivers/Slack/SlackNotifier.groovy',
        'Drivers/Unifi/AccessDoorbell.groovy',
        'Drivers/Enerwave/ZwnSc7SceneController.groovy',
        'Drivers/Heatit/HeatitZTrm2fx.groovy',
        'Drivers/DreamScreen/DreamScreen.groovy',
        'Drivers/MagicHome/MagicHomeWs28xx.groovy',
        'Drivers/WiOnWoodsEcoPlugs/SmartOutlet.groovy',
        'Apps/Lightify/LightifyBridgeIntegration.groovy',
        'Apps/SimpleSprinklers/SimpleSprinklers.groovy',
        'Apps/SuperFan/SuperFan.groovy'
    ] as Set

    /** hubitat_ci's API whitelist predates these helpers, so any script using them must run unrestricted. */
    static List<Flags> flagsFor(File f) {
        def extra = f.path in KNOWN ? RELAXED : []
        def newHelpers = f.text =~ /\b(ColorUtils|HexUtils|InterfaceUtils)\b/
        LEGACY + extra + (newHelpers.find() ? [Flags.DontRestrictGroovy] : [])
    }

    /** Paths of all scripts under dir whose source defines a method called name (textual check, good enough for def/private). */
    static List<String> defining(String dir, String name) {
        scripts(dir).findAll { it.text =~ /(?m)^\s*(?:private\s+|def\s+)+${name}\s*\(/ }*.path
    }

    static List<File> scripts(String dir) {
        def out = []
        new File(dir).eachFileRecurse { if (it.name.endsWith('.groovy')) out << it }
        out.sort()
    }
}
