package com.alameen.fullquran

import org.json.JSONObject

data class AdsConfig(
    val format: String,
    val enabled: Boolean,
    val unitFirst: String,
    val unitReturn: String,
    val unitBanner: String,
    val unitAppOpen: String,
    val returnMinGapSec: Int,
    val firstWaitMs: Long
) {
    val effective: String get() = if (!enabled) "none" else format
    val showInterstitial: Boolean
        get() = effective == "interstitial" || effective == "banner+interstitial"
    val showBanner: Boolean
        get() = effective == "banner" || effective == "banner+interstitial"
    val showAppOpen: Boolean get() = effective == "app_open"
}

object AdsConfigs {
    val FORMATS = setOf("interstitial", "banner", "app_open", "none", "banner+interstitial")
    private val UNIT_RE = Regex("^ca-app-pub-[0-9]{16}/[0-9]{10}$")

    /** Values built into the app (from ads.conf at build time). */
    fun defaults(): AdsConfig {
        val fmt = if (BuildConfig.ADS_FORMAT in FORMATS) BuildConfig.ADS_FORMAT else "banner"
        return AdsConfig(
            format = fmt,
            enabled = true,
            unitFirst = BuildConfig.UNIT_FIRST,
            unitReturn = BuildConfig.UNIT_RETURN,
            unitBanner = BuildConfig.UNIT_BANNER,
            unitAppOpen = BuildConfig.UNIT_APP_OPEN,
            returnMinGapSec = BuildConfig.RETURN_MIN_GAP_SEC,
            firstWaitMs = BuildConfig.FIRST_WAIT_MS
        )
    }

    /** Apply remote JSON on top of [base]; every invalid value is ignored. */
    fun merge(base: AdsConfig, j: JSONObject): AdsConfig {
        fun unit(key: String, current: String): String {
            val v = j.optString(key, "")
            return if (UNIT_RE.matches(v)) v else current
        }
        val fmt = j.optString("format", "")
        val enabledRaw = j.opt("enabled")
        val gap = if (j.has("return_min_gap_sec")) j.optInt("return_min_gap_sec", -1) else -1
        val wait = if (j.has("first_wait_ms")) j.optLong("first_wait_ms", -1L) else -1L
        return base.copy(
            format = if (fmt in FORMATS) fmt else base.format,
            enabled = if (enabledRaw is Boolean) enabledRaw else base.enabled,
            unitFirst = unit("unit_first", base.unitFirst),
            unitReturn = unit("unit_return", base.unitReturn),
            unitBanner = unit("unit_banner", base.unitBanner),
            unitAppOpen = unit("unit_app_open", base.unitAppOpen),
            returnMinGapSec = if (gap in 0..3600) gap else base.returnMinGapSec,
            firstWaitMs = if (wait in 0L..30000L) wait else base.firstWaitMs
        )
    }
}
