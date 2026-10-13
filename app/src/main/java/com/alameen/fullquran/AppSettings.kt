package com.alameen.fullquran

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppSettings {
    private lateinit var prefs: SharedPreferences

    var lang by mutableStateOf("en")
        private set
    var autoNext by mutableStateOf(true)
        private set
    var favorites by mutableStateOf<Set<Int>>(emptySet())
        private set

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        lang = prefs.getString("lang", "en") ?: "en"
        autoNext = prefs.getBoolean("auto_next", true)
        favorites = (prefs.getStringSet("favs", emptySet()) ?: emptySet())
            .mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun changeLang(l: String) {
        lang = l
        prefs.edit().putString("lang", l).apply()
    }

    fun changeAutoNext(v: Boolean) {
        autoNext = v
        prefs.edit().putBoolean("auto_next", v).apply()
        QuranApp.instance.controller.applyAutoNext()
    }

    fun toggleFav(n: Int) {
        favorites = if (n in favorites) favorites - n else favorites + n
        prefs.edit().putStringSet("favs", favorites.map { it.toString() }.toSet()).apply()
    }
}

/** Tiny translation helper: English default, Hausa optional. */
fun tr(en: String, ha: String): String = if (AppSettings.lang == "ha") ha else en
