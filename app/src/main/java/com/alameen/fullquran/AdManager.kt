package com.alameen.fullquran

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

object AdManager {
    private lateinit var app: Application
    private val main = Handler(Looper.getMainLooper())
    private var appStart = 0L

    var cfg by mutableStateOf(AdsConfigs.defaults())
        private set
    var adsReady by mutableStateOf(false)
        private set

    private var sdkOk = false
    private var cfgOk = false
    private var launchDone = false
    private var showing = false
    private var lastShown = 0L

    private const val HOUR = 60 * 60 * 1000L

    // ---------- Preloaded ad slots ----------
    private class Slot(val expiryMs: Long, val loader: (Slot) -> Unit) {
        var ad: Any? = null
        var loadedAt = 0L
        var loading = false
        var attempts = 0
        private val expire = Runnable { ad = null; request() }

        fun isReady(): Boolean =
            ad != null && SystemClock.elapsedRealtime() - loadedAt < expiryMs

        fun request() {
            if (loading || isReady()) return
            if (!adsReady) return
            ad = null
            loading = true
            try {
                loader(this)
            } catch (t: Throwable) {
                onFailed()
            }
        }

        fun onLoaded(a: Any) {
            main.post {
                ad = a
                loadedAt = SystemClock.elapsedRealtime()
                loading = false
                attempts = 0
                main.removeCallbacks(expire)
                main.postDelayed(expire, expiryMs)   // reload after expiry
            }
        }

        fun onFailed() {
            main.post {
                loading = false
                attempts++
                val delayMs = min(60_000L, 2_000L shl min(attempts, 5))   // backoff
                main.postDelayed({ request() }, delayMs)
            }
        }

        fun take(): Any? {
            val a = ad
            ad = null
            main.removeCallbacks(expire)
            return a
        }
    }

    private val first = Slot(HOUR) { s -> loadInterstitial(cfg.unitFirst, s) }
    private val ret = Slot(HOUR) { s -> loadInterstitial(cfg.unitReturn, s) }
    private val appOpen = Slot(4 * HOUR) { s -> loadAppOpen(cfg.unitAppOpen, s) }

    private fun loadInterstitial(unit: String, s: Slot) {
        InterstitialAd.load(app, unit, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { s.onLoaded(ad) }
                override fun onAdFailedToLoad(error: LoadAdError) { s.onFailed() }
            })
    }

    private fun loadAppOpen(unit: String, s: Slot) {
        AppOpenAd.load(app, unit, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) { s.onLoaded(ad) }
                override fun onAdFailedToLoad(error: LoadAdError) { s.onFailed() }
            })
    }

    // ---------- Startup: SDK (background) + remote config (background) ----------
    fun init(application: Application) {
        app = application
        appStart = SystemClock.elapsedRealtime()
        val prefs = app.getSharedPreferences("ads_cache", Context.MODE_PRIVATE)

        // last good copy (used when offline)
        prefs.getString("last_good_json", null)?.let { saved ->
            try { cfg = AdsConfigs.merge(AdsConfigs.defaults(), JSONObject(saved)) } catch (_: Throwable) {}
        }

        application.registerActivityLifecycleCallbacks(lifecycle)

        Thread {
            try {
                MobileAds.initialize(app) { main.post { sdkOk = true; maybeStart() } }
            } catch (_: Throwable) { }
        }.start()

        Thread {
            val text = fetchRemote(BuildConfig.REMOTE_CONFIG_URL)
            var parsed: JSONObject? = null
            if (text != null) {
                try { parsed = JSONObject(text) } catch (_: Throwable) {}
            }
            main.post {
                if (parsed != null) {
                    cfg = AdsConfigs.merge(AdsConfigs.defaults(), parsed)
                    prefs.edit().putString("last_good_json", text).apply()
                }
                cfgOk = true      // read OR failed: either way we may proceed
                maybeStart()
            }
        }.start()
    }

    private fun fetchRemote(url: String): String? {
        if (!url.startsWith("https://")) return null
        var conn: HttpURLConnection? = null
        return try {
            conn = URL("$url?t=${System.currentTimeMillis()}").openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.instanceFollowRedirects = true
            if (conn.responseCode != 200) return null
            val limit = 8 * 1024
            val buf = java.io.ByteArrayOutputStream()
            conn.inputStream.use { ins ->
                val tmp = ByteArray(1024)
                while (true) {
                    val n = ins.read(tmp)
                    if (n < 0) break
                    buf.write(tmp, 0, n)
                    if (buf.size() > limit) return null   // too big: ignore
                }
            }
            buf.toString("UTF-8")
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun maybeStart() {
        if (sdkOk && cfgOk && !adsReady) {
            adsReady = true
            if (cfg.showInterstitial) { first.request(); ret.request() }
            if (cfg.showAppOpen) appOpen.request()
        }
    }

    // ---------- Showing ads ----------
    private fun gapOk(): Boolean {
        val gap = cfg.returnMinGapSec
        return gap <= 0 || lastShown == 0L ||
            SystemClock.elapsedRealtime() - lastShown >= gap * 1000L
    }

    private fun showSlot(activity: Activity, slot: Slot, onClosed: () -> Unit): Boolean {
        if (showing || !slot.isReady()) {
            slot.request()
            return false
        }
        val ad = slot.take() ?: return false
        val done = {
            showing = false
            lastShown = SystemClock.elapsedRealtime()
            slot.request()          // preload the next one
            onClosed()
        }
        val cb = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { done() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { done() }
        }
        return try {
            showing = true
            when (ad) {
                is InterstitialAd -> { ad.fullScreenContentCallback = cb; ad.show(activity) }
                is AppOpenAd -> { ad.fullScreenContentCallback = cb; ad.show(activity) }
                else -> { showing = false; return false }
            }
            true
        } catch (_: Throwable) {
            showing = false
            slot.request()
            false
        }
    }

    /**
     * Called after the splash. Opens the app (onDone) after the user closes the ad,
     * or right away if the ad is not ready within first_wait_ms / no internet.
     */
    fun showFirstAd(activity: ComponentActivity, onDone: () -> Unit) {
        var finished = false
        val finish = {
            if (!finished) { finished = true; onDone() }
        }
        val tick = object : Runnable {
            override fun run() {
                if (finished) return
                val timeUp = SystemClock.elapsedRealtime() >= appStart + cfg.firstWaitMs
                if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    finish(); return
                }
                if (!adsReady) {
                    if (timeUp) finish() else main.postDelayed(this, 150)
                    return
                }
                val slot = when {
                    cfg.showInterstitial -> first
                    cfg.showAppOpen -> appOpen
                    else -> null
                }
                if (slot == null) { finish(); return }
                if (showSlot(activity, slot) { finish() }) return
                if (timeUp) finish() else main.postDelayed(this, 150)
            }
        }
        tick.run()
    }

    fun markLaunchDone() { launchDone = true }

    private fun onReturn(activity: Activity) {
        if (!launchDone || showing || !adsReady || !gapOk()) return
        val slot = when {
            cfg.showInterstitial -> ret
            cfg.showAppOpen -> appOpen
            else -> return
        }
        showSlot(activity, slot) { }
    }

    // ---------- "Left the app and came back" detection ----------
    private var started = 0
    private var left = false
    private var pendingReturn = false
    private val leaveRunnable = Runnable { if (started == 0) left = true }

    private fun isAdActivity(a: Activity) = a.javaClass.name.startsWith("com.google.android.gms.ads")

    private val lifecycle = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            started++
            main.removeCallbacks(leaveRunnable)
            if (left) {
                left = false
                pendingReturn = !isAdActivity(activity)
            }
        }

        override fun onActivityResumed(activity: Activity) {
            if (pendingReturn && !isAdActivity(activity)) {
                pendingReturn = false
                onReturn(activity)
            }
        }

        override fun onActivityStopped(activity: Activity) {
            started--
            if (started <= 0) {
                started = 0
                main.postDelayed(leaveRunnable, 700)   // ignore rotation
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }
}
