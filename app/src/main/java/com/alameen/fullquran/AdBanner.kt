package com.alameen.fullquran

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Anchored adaptive banner. Sits in a Column BELOW the content, so it reserves its own
 * space and never covers buttons. If it fails to load, it disappears and the space collapses.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (!AdManager.adsReady || !AdManager.cfg.showBanner) return
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    if (failed) return

    val adView = remember {
        AdView(context).apply {
            val dm = context.resources.displayMetrics
            val widthDp = (dm.widthPixels / dm.density).toInt()
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = AdManager.cfg.unitBanner
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) { failed = true }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    val owner = context as? LifecycleOwner
    DisposableEffect(adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_RESUME -> adView.resume()
                else -> {}
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(factory = { adView }, modifier = modifier.fillMaxWidth())
}
