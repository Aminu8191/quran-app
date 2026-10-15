package com.alameen.fullquran

import android.app.Application
import android.content.Context
import android.net.Uri
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

    /** Surah numbers inside the player's playlist, in the same order as the player's items. */
    private val playlist = mutableListOf<Int>()
    private var playlistReady = false
    private var pendingPlay = 0

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

    // ---------------- playlist (only surahs that are on the phone) ----------------
    private fun itemFor(n: Int): MediaItem {
        val s = SURAHS[n - 1]
        return MediaItem.Builder()
            .setMediaId("%03d".format(n))
            .setUri(Uri.fromFile(AudioStore.file(n)))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("${s.en}  |  ${s.ar}")
                    .setArtist(RECITER)
                    .setAlbumTitle("Full Quran")
                    .build()
            )
            .build()
    }

    private fun ensurePlaylist() {
        if (playlistReady) return
        val nums = AudioStore.downloaded.sorted()
        if (nums.isEmpty()) return
        playlist.clear()
        playlist.addAll(nums)
        player.setMediaItems(nums.map { itemFor(it) }, false)
        player.prepare()
        playlistReady = true
    }

    fun onSurahDownloaded(n: Int) {
        if (!playlistReady || n in playlist) return
        val idx = playlist.count { it < n }
        playlist.add(idx, n)
        player.addMediaItem(idx, itemFor(n))
    }

    fun onSurahDeleted(n: Int) {
        if (!playlistReady) return
        val idx = playlist.indexOf(n)
        if (idx < 0) return
        playlist.removeAt(idx)
        player.removeMediaItem(idx)
    }

    fun resetPlaylist() {
        player.pause()
        player.clearMediaItems()
        playlist.clear()
        playlistReady = false
        currentSurah = 0
        prefs.edit().remove("surah").remove("pos").apply()
    }

    // ---------------- controls ----------------
    fun play(n: Int) {
        if (!AudioStore.has(n)) return
        ensurePlaylist()
        val idx = playlist.indexOf(n)
        if (idx < 0) return
        repeatDone = 0
        player.seekTo(idx, 0)
        applyRepeatMode()
        player.play()
    }

    /** Tap on a surah: play it if it is on the phone, otherwise download it and play when ready. */
    fun downloadAndPlay(n: Int) {
        pendingPlay = n
        AudioStore.download(n) { ok ->
            if (ok && pendingPlay == n) {
                pendingPlay = 0
                play(n)
            }
        }
    }

    fun toggle() {
        if (currentSurah == 0) {
            AudioStore.downloaded.minOrNull()?.let { play(it) }
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

    // ---------------- resume (continue where you stopped) ----------------
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
        if (n == 0 || !AudioStore.has(n)) return
        ensurePlaylist()
        val idx = playlist.indexOf(n)
        if (idx < 0) return
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
