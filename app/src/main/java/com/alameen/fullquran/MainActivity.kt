package com.alameen.fullquran

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            startService(Intent(this, PlaybackService::class.java))
        } catch (_: Exception) {
        }
        setContent {
            QuranTheme { Root(this) }
        }
    }
}

@Composable
fun Root(activity: MainActivity) {
    var splashDone by rememberSaveable { mutableStateOf(false) }
    var screen by rememberSaveable { mutableStateOf("home") }

    // keeps the scroll position of the surah list when going to the player and back
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (QuranApp.instance.controller.currentSurah - 1).coerceAtLeast(0)
    )

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    if (!splashDone) {
        SplashScreen()
        LaunchedEffect(Unit) {
            delay(1200)   // splash is shown first, ads never appear on top of it
            AdManager.showFirstAd(activity) {
                AdManager.markLaunchDone()
                splashDone = true
            }
        }
    } else {
        LaunchedEffect(Unit) {
            AdManager.markLaunchDone()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        when (screen) {
            "player" -> PlayerScreen { screen = "home" }
            "settings" -> SettingsScreen { screen = "home" }
            else -> HomeScreen(
                listState = listState,
                onOpenPlayer = { screen = "player" },
                onOpenSettings = { screen = "settings" }
            )
        }
    }
}
