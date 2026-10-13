package com.alameen.fullquran

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val SUPPORT_EMAIL = "aminulawanzakariyyah@gmail.com"
private const val TELEGRAM_URL = "https://t.me/al_ameen_developer"
private val OnGold = Color(0xFF1B1400)

// ============================== SPLASH ==============================
@Composable
fun SplashScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Surface1, Bg))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.logo), null,
            Modifier.size(190.dp).clip(CircleShape).border(3.dp, Gold, CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(24.dp))
        Text("Full Quran", color = Gold, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(RECITER, color = TextMain, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        CircularProgressIndicator(color = Gold, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        Spacer(Modifier.height(36.dp))
        Text("Powered by Al-ameen", color = TextDim, fontSize = 12.sp)
    }
}

// ============================== HOME ==============================
@Composable
fun HomeScreen(onOpenPlayer: () -> Unit, onOpenSettings: () -> Unit) {
    val ctrl = QuranApp.instance.controller
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    val q = normalizeName(query)
    val favs = AppSettings.favorites
    val list = SURAHS.filter { s ->
        (tab == 0 || s.n in favs) &&
            (q.isEmpty() || s.n.toString() == q || normalizeName(s.en).contains(q) ||
                s.ar.contains(query.trim()))
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        // header
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Surface1, Bg)))
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(R.drawable.logo), null,
                Modifier.size(58.dp).clip(CircleShape).border(2.dp, Gold, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Full Quran", "Cikakken Alƙur'ani"), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(RECITER, color = TextMain, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = tr("Settings", "Saituna"), tint = Gold)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(tr("Search surah (name or number)", "Nemo sura (suna ko lamba)")) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text(tr("All", "Duka")) })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text(tr("Favorites", "Waɗanda aka fi so")) })
        }

        if (list.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (tab == 1 && query.isEmpty()) tr("No favorites yet. Tap the heart on a surah.", "Babu wanda aka fi so tukuna. Danna zuciya a kan sura.")
                    else tr("No surah found", "Ba a samu sura ba"),
                    color = TextDim, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(list, key = { it.n }) { s ->
                    val has = ctrl.hasAudio(s.n)
                    SurahRow(
                        s = s,
                        hasAudio = has,
                        current = ctrl.currentSurah == s.n,
                        playing = ctrl.currentSurah == s.n && ctrl.isPlaying,
                        fav = s.n in favs,
                        onClick = { ctrl.play(s.n); onOpenPlayer() },
                        onFav = { AppSettings.toggleFav(s.n) }
                    )
                }
            }
        }

        if (ctrl.currentSurah != 0) MiniPlayer(onOpenPlayer)
        BannerAd()
    }
}

@Composable
fun SurahRow(
    s: Surah, hasAudio: Boolean, current: Boolean, playing: Boolean, fav: Boolean,
    onClick: () -> Unit, onFav: () -> Unit
) {
    val a = if (hasAudio) 1f else 0.45f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (current) CardColor else Surface1)
            .clickable(enabled = hasAudio, onClick = onClick)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(if (current) Gold else Color(0xFF145A3F)),
            contentAlignment = Alignment.Center
        ) {
            Text("${s.n}", color = if (current) OnGold else GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title(), color = TextMain.copy(alpha = a), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = when {
                !hasAudio -> tr("No audio", "Babu audio")
                playing -> "▶ " + tr("Playing", "Ana kunnawa")
                else -> "${s.ayahs} " + tr("ayahs", "ayoyi")
            }
            Text(sub, color = if (playing) Gold else TextDim.copy(alpha = a), fontSize = 12.sp)
        }
        Text(s.ar, color = Gold.copy(alpha = a), fontSize = 22.sp)
        IconButton(onClick = onFav) {
            Icon(
                if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (fav) Gold else TextDim
            )
        }
    }
}

@Composable
fun MiniPlayer(onClick: () -> Unit) {
    val c = QuranApp.instance.controller
    val s = SURAHS.getOrNull(c.currentSurah - 1) ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 14.dp)   // gap above banner
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(CardColor, Surface1)))
            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.logo), null, Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title(), color = TextMain, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(RECITER, color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Gold).clickable { c.toggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(if (c.isPlaying) R.drawable.ic_pause else R.drawable.ic_play), null, tint = OnGold)
        }
    }
}

// ============================== PLAYER ==============================
private fun fmt(ms: Long): String {
    val t = (ms / 1000).toInt()
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val c = QuranApp.instance.controller
    BackHandler(onBack = onBack)
    val s = SURAHS.getOrNull(c.currentSurah - 1)

    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var showRepeat by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!dragging) {
                pos = c.player.currentPosition.coerceAtLeast(0L)
                dur = c.player.duration.coerceAtLeast(0L)
            }
            delay(400)
        }
    }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Surface1, Bg)))) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Gold)
            }
            Text(tr("Now playing", "Ana kunnawa yanzu"), color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Image(
                painterResource(R.drawable.logo), null,
                Modifier.size(200.dp).clip(CircleShape).border(3.dp, Gold, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(20.dp))
            Text(s?.ar ?: "", color = Gold, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(s?.title() ?: "", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(RECITER, color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            Slider(
                value = (if (dragging) dragValue else pos.toFloat()).coerceIn(0f, maxOf(dur, 1L).toFloat()),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    c.player.seekTo(dragValue.toLong())
                    pos = dragValue.toLong()
                    dragging = false
                },
                valueRange = 0f..maxOf(dur, 1L).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Color(0x33FFFFFF)
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(if (dragging) dragValue.toLong() else pos), color = TextDim, fontSize = 12.sp)
                Text(fmt(dur), color = TextDim, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(onClick = { c.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(painterResource(R.drawable.ic_prev), null, tint = TextMain, modifier = Modifier.size(34.dp))
                }
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(Gold).clickable { c.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(if (c.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        null, tint = OnGold, modifier = Modifier.size(42.dp)
                    )
                }
                IconButton(onClick = { c.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(painterResource(R.drawable.ic_next), null, tint = TextMain, modifier = Modifier.size(34.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            if (c.repeatTarget != 1) {
                val total = if (c.repeatTarget == 0) "∞" else "${c.repeatTarget}"
                Text(
                    tr("Playing ${c.repeatDone + 1} of $total", "Ana kunnawa karo na ${c.repeatDone + 1} cikin $total"),
                    color = Gold, fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val rep = when (c.repeatTarget) { 1 -> tr("Off", "A kashe"); 0 -> "∞"; else -> "${c.repeatTarget}x" }
                AssistChip(onClick = { showRepeat = true }, label = { Text(tr("Repeat", "Maimaitawa") + ": " + rep) })
                val sl = c.sleepRemainingSec
                val slText = if (sl > 0) "%d:%02d".format(sl / 60, sl % 60) else tr("Off", "A kashe")
                AssistChip(onClick = { showSleep = true }, label = { Text(tr("Sleep", "Barci") + ": " + slText) })
                val isFav = s != null && s.n in AppSettings.favorites
                IconButton(onClick = { s?.let { AppSettings.toggleFav(it.n) } }) {
                    Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFav) Gold else TextDim)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        BannerAd()
    }

    if (showRepeat) RepeatDialog(c.repeatTarget, { c.setRepeat(it); showRepeat = false }, { showRepeat = false })
    if (showSleep) SleepDialog({ c.setSleepMinutes(it); showSleep = false }, { showSleep = false })
}

@Composable
private fun OptionButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 2.dp)) { Text(label, fontSize = 13.sp, maxLines = 1) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 2.dp)) { Text(label, fontSize = 13.sp, maxLines = 1) }
    }
}

@Composable
private fun RepeatDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Close", "Rufe")) } },
        title = { Text(tr("Repeat surah", "Maimaita sura")) },
        text = {
            Column {
                listOf(1, 2, 3, 5, 7, 10, 20, 33, 40, 100).chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { n -> OptionButton("${n}x", current == n, Modifier.weight(1f)) { onPick(n) } }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                OptionButton("∞  " + tr("Forever", "Har abada"), current == 0, Modifier.fillMaxWidth()) { onPick(0) }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = custom,
                        onValueChange = { custom = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text(tr("Any number", "Wani adadi")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val v = custom.toIntOrNull()
                        if (v != null && v >= 1) onPick(v)
                    }) { Text(tr("Set", "Sa")) }
                }
            }
        }
    )
}

@Composable
private fun SleepDialog(onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Close", "Rufe")) } },
        title = { Text(tr("Sleep timer", "Ƙidayar barci")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 15, 30, 45, 60, 90).forEach { m ->
                    OptionButton(
                        if (m == 0) tr("Off", "A kashe") else "$m " + tr("minutes", "mintuna"),
                        false, Modifier.fillMaxWidth()
                    ) { onPick(m) }
                }
            }
        }
    )
}

// ============================== SETTINGS ==============================
private fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(ctx, "No app found to open this link", Toast.LENGTH_SHORT).show()
    }
}

private fun openEmail(ctx: Context) {
    try {
        val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
        i.putExtra(Intent.EXTRA_SUBJECT, "Full Quran App")
        ctx.startActivity(i)
    } catch (_: Exception) {
        Toast.makeText(ctx, SUPPORT_EMAIL, Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp))
}

@Composable
private fun CardColumn(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp)).background(Surface1)
    ) { content() }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Gold)
            }
            Text(tr("Settings", "Saituna"), color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionTitle(tr("Language", "Harshe"))
            CardColumn {
                listOf("en" to "English", "ha" to "Hausa").forEach { (code, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { AppSettings.changeLang(code) }.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = AppSettings.lang == code, onClick = { AppSettings.changeLang(code) })
                        Text(label, color = TextMain, fontSize = 16.sp)
                    }
                }
            }

            SectionTitle(tr("Playback", "Kunna audio"))
            CardColumn {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Continue to next surah", "Ci gaba da sura ta gaba"), color = TextMain, fontSize = 16.sp)
                        Text(
                            tr("Automatically play the next surah when one ends", "A kunna sura ta gaba kai tsaye idan wannan ta ƙare"),
                            color = TextDim, fontSize = 12.sp
                        )
                    }
                    Switch(checked = AppSettings.autoNext, onCheckedChange = { AppSettings.changeAutoNext(it) })
                }
            }

            SectionTitle(tr("Contact / Support", "Tuntuɓa / Taimako"))
            CardColumn {
                Column(Modifier.fillMaxWidth().clickable { openEmail(ctx) }.padding(16.dp)) {
                    Text(tr("Email the developer", "Aika wa mai app imel"), color = TextMain, fontSize = 16.sp)
                    Text(SUPPORT_EMAIL, color = Gold, fontSize = 13.sp)
                }
            }

            SectionTitle(tr("Community", "Al'umma"))
            CardColumn {
                Column(Modifier.fillMaxWidth().clickable { openUrl(ctx, TELEGRAM_URL) }.padding(16.dp)) {
                    Text(tr("Join our Telegram channel", "Shiga tashar Telegram ɗinmu"), color = TextMain, fontSize = 16.sp)
                    Text(
                        tr("Get updates, the latest version and more info", "Samu sabuntawa, sabuwar sigar app da ƙarin bayani"),
                        color = TextDim, fontSize = 12.sp
                    )
                    Text(TELEGRAM_URL, color = Gold, fontSize = 13.sp)
                }
            }

            SectionTitle(tr("About", "Game da app"))
            CardColumn {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Full Quran – $RECITER", color = TextMain, fontSize = 15.sp)
                    Text(tr("Version", "Sigar") + " " + BuildConfig.VERSION_NAME, color = TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Powered by Al-ameen", color = Gold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        BannerAd()
    }
}
