package com.alameen.fullquran

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.Executors

/** Surahs the Sheikh did not record (no audio exists for them). */
val NO_AUDIO = setOf(76, 86, 88, 89, 94, 99, 110)
val TOTAL_AUDIO = 115 - NO_AUDIO.size
fun hasRemoteAudio(n: Int): Boolean = n in 1..115 && n !in NO_AUDIO

/** Downloads surah audio from the GitHub release and keeps it on the phone (works offline afterwards). */
object AudioStore {
    private lateinit var app: Application
    private lateinit var dir: File
    private val main = Handler(Looper.getMainLooper())
    private val pool = Executors.newSingleThreadExecutor()

    var downloaded by mutableStateOf<Set<Int>>(emptySet())
        private set

    /** surah -> progress 0..1 (negative = waiting / unknown size). Present = download in progress. */
    val progress = mutableStateMapOf<Int, Float>()

    var bulkRunning by mutableStateOf(false)
        private set
    var bulkDone by mutableIntStateOf(0)
        private set
    var bulkTotal by mutableIntStateOf(0)
        private set

    private val cancelled = Collections.synchronizedSet(HashSet<Int>())
    private val waiters = HashMap<Int, MutableList<(Boolean) -> Unit>>()

    fun init(application: Application) {
        app = application
        val base = app.getExternalFilesDir(null) ?: app.filesDir
        dir = File(base, "audio")
        dir.mkdirs()
        dir.listFiles().orEmpty().forEach { if (it.name.endsWith(".part")) it.delete() }
        downloaded = dir.listFiles().orEmpty()
            .filter { it.name.endsWith(".opus") && it.length() > 0 }
            .mapNotNull { it.name.substringBefore('.').toIntOrNull() }
            .toSet()
    }

    fun file(n: Int): File = File(dir, "%03d.opus".format(n))
    fun has(n: Int): Boolean = n in downloaded
    fun totalBytes(): Long {
        var sum = 0L
        for (n in downloaded) sum += file(n).length()
        return sum
    }

    /** Download one surah. [cb] is called on the main thread with true when it is on the phone. */
    fun download(n: Int, cb: ((Boolean) -> Unit)? = null) {
        if (!hasRemoteAudio(n)) return
        if (has(n)) { cb?.invoke(true); return }
        if (cb != null) waiters.getOrPut(n) { mutableListOf() }.add(cb)
        if (progress.containsKey(n)) return
        progress[n] = -1f
        cancelled.remove(n)
        pool.execute {
            val ok = try { fetch(n) } catch (t: Throwable) { false }
            main.post {
                progress.remove(n)
                if (ok) {
                    downloaded = downloaded + n
                    QuranApp.instance.controller.onSurahDownloaded(n)
                } else if (!cancelled.contains(n)) {
                    toast(tr("Download failed. Check your internet.", "Saukewa ya gaza. Ka duba intanet ɗinka."))
                }
                cancelled.remove(n)
                waiters.remove(n)?.forEach { it(ok) }
            }
        }
    }

    private fun fetch(n: Int): Boolean {
        if (cancelled.contains(n)) return false
        val target = file(n)
        val part = File(dir, "%03d.part".format(n))
        var conn: HttpURLConnection? = null
        try {
            conn = URL(BuildConfig.AUDIO_BASE_URL + "%03d.opus".format(n)).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.instanceFollowRedirects = true
            if (conn.responseCode != 200) return false
            val total = conn.contentLengthLong
            var got = 0L
            var lastPost = 0L
            conn.inputStream.use { ins ->
                FileOutputStream(part).use { out ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        if (cancelled.contains(n)) { part.delete(); return false }
                        val r = ins.read(buf)
                        if (r < 0) break
                        out.write(buf, 0, r)
                        got += r
                        val now = SystemClock.elapsedRealtime()
                        if (total > 0 && now - lastPost > 200) {
                            lastPost = now
                            val p = got.toFloat() / total.toFloat()
                            main.post { if (progress.containsKey(n)) progress[n] = p }
                        }
                    }
                }
            }
            if ((total > 0 && got != total) || got < 1000) { part.delete(); return false }
            if (!part.renameTo(target)) { part.delete(); return false }
            return true
        } finally {
            conn?.disconnect()
        }
    }

    fun cancel(n: Int) { cancelled.add(n) }

    fun cancelAll() {
        for (n in progress.keys.toList()) cancelled.add(n)
    }

    /** Download every surah that is not on the phone yet. */
    fun downloadAll() {
        if (bulkRunning) return
        val todo = (1..115).filter { hasRemoteAudio(it) && !has(it) && !progress.containsKey(it) }
        if (todo.isEmpty()) return
        bulkRunning = true
        bulkDone = 0
        bulkTotal = todo.size
        var remaining = todo.size
        for (n in todo) {
            download(n) {
                bulkDone++
                remaining--
                if (remaining <= 0) bulkRunning = false
            }
        }
    }

    fun delete(n: Int) {
        if (n == QuranApp.instance.controller.currentSurah) return
        file(n).delete()
        downloaded = downloaded - n
        QuranApp.instance.controller.onSurahDeleted(n)
    }

    fun deleteAll() {
        cancelAll()
        QuranApp.instance.controller.resetPlaylist()
        dir.listFiles().orEmpty().forEach { if (it.name.endsWith(".opus")) it.delete() }
        downloaded = emptySet()
    }

    private fun toast(msg: String) {
        Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
    }
}
