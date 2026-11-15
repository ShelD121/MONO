package com.mono.music

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private data class Track(val title: String, val uri: String)
private val gray = Color(0xFFF1F2F4)
private val card = Color(0xFFE3E5E8)
private val dark = Color(0xFF27292C)

@Composable
fun MusicApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mono", Context.MODE_PRIVATE) }
    val tracks = remember { mutableStateListOf<Track>() }
    val favorites = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("favorites", emptySet()) ?: emptySet()) } }
    val player = remember { ExoPlayer.Builder(context).build() }
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var current by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    val labels = listOf("Главная", "Поиск", "Избранное", "Моя музыка", "Настройки")
    val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.Favorite, Icons.Default.LibraryMusic, Icons.Default.Settings)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.coerceAtLeast(0)
            kotlinx.coroutines.delay(500)
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Аудиофайл"
            if (tracks.none { it.uri == uri.toString() }) tracks.add(Track(title, uri.toString()))
        }
        prefs.edit().putString("tracks", tracks.joinToString("\n") { it.title.replace("\t", " ") + "\t" + it.uri }).apply()
    }
    LaunchedEffect(Unit) {
        prefs.getString("tracks", "")?.lines()?.filter { it.contains("\t") }?.forEach {
            val parts = it.split("\t", limit = 2)
            if (parts.size == 2 && tracks.none { t -> t.uri == parts[1] }) tracks.add(Track(parts[0], parts[1]))
        }
    }
    fun play(track: Track) {
        current = track
        player.setMediaItem(MediaItem.fromUri(Uri.parse(track.uri)))
        player.prepare()
        player.play()
    }
    fun toggleFavorite(track: Track) {
        if (favorites.contains(track.uri)) favorites.remove(track.uri) else favorites.add(track.uri)
        prefs.edit().putStringSet("favorites", favorites.toSet()).apply()
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = dark, background = gray, surface = card)) {
        if (expanded && current != null) {
            Column(Modifier.fillMaxSize().background(gray).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = { expanded = false }) { Icon(Icons.Default.KeyboardArrowDown, "Закрыть") }
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(card, RoundedCornerShape(34.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(90.dp), tint = dark)
                }
                Spacer(Modifier.height(36.dp))
                Text(current!!.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.height(30.dp))
                Slider(value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                    onValueChange = { if (duration > 0) player.seekTo((it * duration).toLong()) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(time(position), fontSize = 12.sp)
                    Text(time(duration), fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { player.seekToPreviousMediaItem() }) { Icon(Icons.Default.SkipPrevious, "Предыдущий") }
                    Spacer(Modifier.width(24.dp))
                    FilledIconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }, modifier = Modifier.size(72.dp)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Пуск/пауза", modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.width(24.dp))
                    IconButton(onClick = { player.seekToNextMediaItem() }) { Icon(Icons.Default.SkipNext, "Следующий") }
                }
                Spacer(Modifier.weight(1f))
            }
        } else {
            Scaffold(containerColor = gray, bottomBar = {
                Column {
                    if (current != null) Surface(shape = RoundedCornerShape(20.dp), color = card, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Row(Modifier.clickable { expanded = true }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Album()
                            Spacer(Modifier.width(12.dp))
                            Text(current!!.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Пуск/пауза")
                            }
                        }
                    }
                    NavigationBar(containerColor = Color(0xFFE8E9EC)) {
                        labels.forEachIndexed { index, label ->
                            NavigationBarItem(selected = tab == index, onClick = { tab = index },
                                icon = { Icon(icons[index], label) }, label = { Text(label, fontSize = 9.sp) })
                        }
                    }
                }
            }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(22.dp))
                    Text("MONO", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Spacer(Modifier.height(26.dp))
                    Text(labels[tab], fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    when (tab) {
                        0 -> {
                            Text("Твоя музыка. Твоё настроение.", color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { picker.launch(arrayOf("audio/*")) }) { Text("Добавить музыку с телефона") }
                            TrackList(tracks, favorites, ::play, ::toggleFavorite)
                        }
                        1 -> {
                            OutlinedTextField(query, { query = it }, label = { Text("Поиск по своей музыке") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            TrackList(tracks.filter { it.title.contains(query, true) }, favorites, ::play, ::toggleFavorite)
                        }
                        2 -> TrackList(tracks.filter { favorites.contains(it.uri) }, favorites, ::play, ::toggleFavorite)
                        3 -> {
                            Button(onClick = { picker.launch(arrayOf("audio/*")) }) { Text("Выбрать аудиофайлы") }
                            TrackList(tracks, favorites, ::play, ::toggleFavorite)
                        }
                        4 -> {
                            Text("Воспроизвести прямую HTTPS-ссылку на аудиофайл", color = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(url, { url = it }, label = { Text("https://…/song.mp3") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(8.dp))
                            Button(enabled = url.startsWith("https://"), onClick = {
                                val track = Track(Uri.parse(url).lastPathSegment ?: "Онлайн-аудио", url)
                                if (tracks.none { it.uri == track.uri }) tracks.add(track)
                                prefs.edit().putString("tracks", tracks.joinToString("\n") { it.title.replace("\t", " ") + "\t" + it.uri }).apply()
                                play(track)
                            }) { Text("Слушать онлайн") }
                            Spacer(Modifier.height(20.dp))
                            Text("MONO · версия 0.2", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
private fun time(ms: Long): String = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)
@Composable private fun Album() {
    Box(Modifier.size(44.dp).background(Color(0xFFC9CDD1), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.MusicNote, null)
    }
}
@Composable private fun TrackList(tracks: List<Track>, favorites: List<String>, onPlay: (Track) -> Unit, onFavorite: (Track) -> Unit) {
    if (tracks.isEmpty()) {
        Spacer(Modifier.height(20.dp))
        Text("Здесь пока пусто — добавь свои треки", color = Color.Gray)
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tracks) { track ->
            Surface(color = card, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().clickable { onPlay(track) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Album()
                    Spacer(Modifier.width(12.dp))
                    Text(track.title, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { onFavorite(track) }) {
                        Icon(if (favorites.contains(track.uri)) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Избранное")
                    }
                }
            }
        }
    }
}
