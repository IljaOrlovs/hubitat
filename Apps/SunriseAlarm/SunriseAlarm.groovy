/**
 *  Sunrise Alarm
 *
 *  Wakes you gently: ramps an RGBW/CT bulb from dim red through amber to warm white
 *  over the chosen ramp length, finishing at 100% at the wake time. Stays on until switched off.
 *  Switching the bulb off during the ramp cancels it.
 */

import groovy.transform.Field

@Field static final int STEP_SECONDS = 30
@Field static final int FADE_SECONDS = 15   // half of STEP_SECONDS (a @Field can't reference another): bulbs fade into each step and settle before the next
@Field static final List DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']

definition(
    name: "Sunrise Alarm",
    namespace: "IljaOrlovs",
    author: "Ilja Orlovs",
    importUrl: "https://raw.githubusercontent.com/IljaOrlovs/hubitat/master/Apps/SunriseAlarm/SunriseAlarm.groovy",
    description: "Light alarm clock: simulates a sunrise on an RGBW bulb.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    section {
        label title: "Name (optional)", required: false
        input "bulbs", "capability.colorControl", title: "Bulb(s) (RGBW with color temperature)", multiple: true, required: true
        input "enabled", "bool", title: "Alarm enabled", defaultValue: true
        input "rampMinutes", "number", title: "Ramp length (minutes)", defaultValue: 20, range: "1..120"
        input "maxCt", "number", title: "Final color temperature (K)", defaultValue: 4000, range: "2000..6500"
        input "logEnable", "bool", title: "Debug logging", defaultValue: false
        input "autoOffTime", "time", title: "Auto power-off time (optional): switch the bulb off then, but only if nothing touched it since the ramp finished", required: false
        input "testNow", "button", title: "Test ramp now (uses the ramp length above)"
    }
    section("<b>Wake time per day</b> (the light reaches 100% at this time)") {
        DAYS.eachWithIndex { name, i ->
            input "on${i}", "bool", title: name, defaultValue: false
            input "time${i}", "time", title: "Time"
        }
    }
    section("Set all days") {
        input "allTime", "time", title: "Time for all days"
        input "applyAll", "button", title: "Apply to all days"
    }
}

def appButtonHandler(String btn) {
    if (btn == "testNow") {
        startRamp()
        return
    }
    if (btn != "applyAll" || !settings.allTime) return
    DAYS.eachWithIndex { n, i ->
        app.updateSetting("on${i}", [type: "bool", value: true])
        app.updateSetting("time${i}", [type: "time", value: settings.allTime])
    }
}

def installed() { initialize() }

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    state.running = false
    if (!enabled) return
    subscribe(bulbs, "switch.off", offHandler)
    subscribe(bulbs, "switch", changeHandler)
    subscribe(bulbs, "level", changeHandler)
    subscribe(bulbs, "colorTemperature", changeHandler)
    if (autoOffTime) schedule("0 ${minuteOfDay(autoOffTime) % 60} ${minuteOfDay(autoOffTime).intdiv(60)} ? * *", "autoOff")
    int ramp = (rampMinutes ?: 20) as int
    DAYS.eachWithIndex { n, i ->
        if (settings."on${i}" && settings."time${i}") {
            schedule(cronFor(i, minuteOfDay(settings."time${i}"), ramp), "startRamp${i}")
        }
    }
}

/** Minutes since midnight from a time input: ISO "…T07:00:00.000±hhmm", already hub-local wall time. */
static int minuteOfDay(String t) {
    Integer.parseInt(t[11..12]) * 60 + Integer.parseInt(t[14..15])
}

/** True if an event after the ramp finished (100%, maxCt) shows someone changed the bulb. */
static boolean isExternalChange(String name, value, int maxCt) {
    if (name == "switch") return value == "off"
    def v = new BigDecimal(value.toString())
    name == "level" ? v != 100 : (v - maxCt).abs() > 150   // 150K: bulbs report colour temperature rounded
}

/** Quartz cron for the ramp start: wake time minus ramp, rolling back a day if it crosses midnight. dayIdx 0 = Monday. */
static String cronFor(int dayIdx, int wakeMinuteOfDay, int ramp) {
    int m = (dayIdx * 1440 + wakeMinuteOfDay - ramp + 10080) % 10080
    int dow = (m.intdiv(1440) + 1) % 7 + 1   // Quartz: 1 = Sunday
    int mod = m % 1440
    "0 ${mod % 60} ${mod.intdiv(60)} ? * ${dow}"
}

/** Pure curve: t in [0,1] -> level, and either hue/sat (red phase) or ct (white phase). */
static Map rampAt(double t, int maxCt) {
    t = Math.max(0d, Math.min(1d, t))
    int level = Math.max(1, Math.round(100 * t * t * t) as int)   // cubic: the eye reads brightness ~ cube root of output, so this ramps evenly
    if (t < 0.4) {
        double p = t / 0.4
        return [level: level, hue: Math.round(2 + 8 * p) as int, sat: Math.round(100 - 20 * p) as int]
    }
    double p = (t - 0.4) / 0.6
    [level: level, ct: Math.round(2000 + (maxCt - 2000) * p) as int]
}

// One handler per day: Hubitat keeps a single schedule per handler name, so a shared one would clobber the other days.
def startRamp0() { startRamp() }
def startRamp1() { startRamp() }
def startRamp2() { startRamp() }
def startRamp3() { startRamp() }
def startRamp4() { startRamp() }
def startRamp5() { startRamp() }
def startRamp6() { startRamp() }

def startRamp() {
    state.untouched = false
    state.step = 0
    state.running = true
    step()
}

def step() {
    if (!state.running) return
    int steps = ((rampMinutes ?: 20) as int) * 60 / STEP_SECONDS
    def r = rampAt(state.step / (double) steps, (maxCt ?: 4000) as int)
    if (logEnable) log.debug "sunrise step ${state.step}/${steps}: ${r}"
    if (r.ct) {
        bulbs*.setColorTemperature(r.ct, r.level, FADE_SECONDS)
    } else {
        bulbs*.setLevel(r.level, FADE_SECONDS)   // before setColor so an off bulb comes on at the ramp level, not its old one
        bulbs*.setColor([hue: r.hue, saturation: r.sat])
    }
    if (state.step >= steps) {
        state.running = false
        state.untouched = true
        state.doneAt = now()
    } else {
        state.step = state.step + 1
        runIn(STEP_SECONDS, "step")
    }
}

def offHandler(evt) {
    if (!state.running) return
    state.running = false
    unschedule("step")
    log.info "Sunrise ramp cancelled: ${evt.device} switched off"
}

def changeHandler(evt) {
    if (!state.untouched || now() < state.doneAt + 60000) return   // 60s: late reports from our own final fade
    if (isExternalChange(evt.name, evt.value, (maxCt ?: 4000) as int)) {
        state.untouched = false
        if (logEnable) log.debug "auto-off disarmed: ${evt.device} ${evt.name} -> ${evt.value}"
    }
}

def autoOff() {
    if (!state.untouched) return
    state.untouched = false
    bulbs*.off()
}
