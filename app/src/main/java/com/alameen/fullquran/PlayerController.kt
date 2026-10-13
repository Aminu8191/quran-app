package com.alameen.fullquran

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

const val RECITER = "Sheikh Usman Abubakar Birnin Kebbi"

class PlayerController(private val app: Application) {

    val player: ExoPlayer = ExoPlayer.Builder(app)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    private val prefs = app.getSharedPreferences("resume", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())

    /** Surah numbers that actually have an audio file inside the app. */
    val available: List<Int> = (app.assets.list("audio") ?: emptyArray())
        .mapNotNull { it.substringBefore('.').toIntOrNull() }
        .filter { it in 1..115 }
        .sorted()

    var currentSurah by mutableIntStateOf(0)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var repeatTarget by mutableIntStateOf(1)      // 0 = forever, N = play N times
        private set
    var repeatDone by mutableIntStateOf(0)
        private set
    var sleepRemainingSec by mutableIntStateOf(0)
        private set

    var speed by mutableFloatStateOf(prefs.getFloat("speed", 1f))
        private set

    private var playlistReady = false

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) saveResume()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSurah = mediaItem?.mediaId?.toIntOrNull() ?: 0
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) repeatDone++ else repeatDone = 0
                applyRepeatMode()
            }
        })
        applyAutoNext()
        player.setPlaybackSpeed(speed)
    }

    fun changeSpeed(v: Float) {
        speed = v
        player.setPlaybackSpeed(v)
        prefs.edit().putFloat("speed", v).apply()
    }

    fun start() {
        main.post(ticker)
    }

    private fun fileName(n: Int) = "%03d".format(n)

    private fun ensurePlaylist() {
        if (playlistReady) return
        val items = available.map { n ->
            val s = SURAHS[n - 1]
            MediaItem.Builder()
                .setMediaId(fileName(n))
                .setUri("asset:///audio/${fileName(n)}.m4a")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("${s.en}  |  ${s.ar}")
                        .setArtist(RECITER)
                        .setAlbumTitle("Full Quran")
                        .build()
                )
                .build()
        }
        player.setMediaItems(items, false)
        player.prepare()
        playlistReady = true
    }

    fun hasAudio(n: Int) = n in available

    fun play(n: Int) {
        val idx = available.indexOf(n)
        if (idx < 0) return
        ensurePlaylist()
        repeatDone = 0
        player.seekTo(idx, 0)
        applyRepeatMode()
        player.play()
    }

    fun toggle() {
        if (currentSurah == 0) {
            available.firstOrNull()?.let { play(it) }
            return
        }
        ensurePlaylist()
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
    fun previous() { if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() }

    fun setRepeat(n: Int) {
        repeatTarget = n
        applyRepeatMode()
    }

    fun applyRepeatMode() {
        val loops = repeatTarget == 0 || repeatDone < repeatTarget - 1
        player.repeatMode = if (loops) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun applyAutoNext() {
        player.pauseAtEndOfMediaItems = !AppSettings.autoNext
    }

    fun setSleepMinutes(min: Int) {
        sleepRemainingSec = min * 60
    }

    // ---- resume (continue where you stopped) ----
    fun saveResume() {
        if (currentSurah == 0) return
        prefs.edit()
            .putInt("surah", currentSurah)
            .putLong("pos", player.currentPosition.coerceAtLeast(0))
            .apply()
    }

    fun restore() {
        val n = prefs.getInt("surah", 0)
        val pos = prefs.getLong("pos", 0L)
        val idx = available.indexOf(n)
        if (idx < 0) return
        ensurePlaylist()
        player.seekTo(idx, pos)
        player.pause()
        currentSurah = n
    }

    // one-second ticker: sleep timer + periodic resume save
    private var tickCount = 0
    private val ticker = object : Runnable {
        override fun run() {
            tickCount++
            if (sleepRemainingSec > 0) {
                sleepRemainingSec--
                if (sleepRemainingSec == 0) player.pause()
            }
            if (player.isPlaying && tickCount % 5 == 0) saveResume()
            main.postDelayed(this, 1000)
        }
    }
}
