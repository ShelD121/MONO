package com.mono.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ink = Color(0xFF26282B)
private val fog = Color(0xFFF0F1F2)
private val panel = Color(0xFFE3E5E7)
private val songs = listOf("Сияй" to "MACAN", "Minor" to "Miyagi & Andy Panda", "Патрон" to "Miyagi & Andy Panda", "Тёмные аллеи" to "Три дня дождя")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MonoApp() }
    }
}
@Composable
fun MonoApp() {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(songs.first()) }
    var playing by remember { mutableStateOf(false) }
    val favorites = remember { mutableStateListOf<String>() }
    val labels = listOf("Главная", "Поиск", "Избранное", "Моя музыка", "Настройки")
    val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.Favorite, Icons.Default.LibraryMusic, Icons.Default.Settings)
    MaterialTheme(colorScheme = lightColorScheme(primary = ink, background = fog, surface = panel)) {
        Scaffold(
            containerColor = fog,
            bottomBar = {
                Column {
                    Surface(color = panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                            AlbumTile()
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(current.first, fontWeight = FontWeight.SemiBold)
                                Text(current.second, fontSize = 12.sp, color = Color.DarkGray)
                            }
                            IconButton(onClick = { playing = !playing }) {
                                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (playing) "Пауза" else "Воспроизвести")
                            }
                        }
                    }
                    NavigationBar(containerColor = Color(0xFFE7E9EB)) {
                        labels.forEachIndexed { index, label ->
                            NavigationBarItem(selected = tab == index, onClick = { tab = index },
                                icon = { Icon(icons[index], contentDescription = label) },
                                label = { Text(label, fontSize = 9.sp) })
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                Text("MONO", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = ink)
                Spacer(Modifier.height(28.dp))
                Text(labels[tab], fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = ink)
                Spacer(Modifier.height(20.dp))
                when (tab) {
                    0 -> {
                        Text("Твоя музыка. Твоё настроение.", color = Color.Gray)
                        Spacer(Modifier.height(20.dp))
                        Text("Недавно слушали", fontWeight = FontWeight.SemiBold)
                        SongList(songs, current, favorites, onPlay = { current = it; playing = true })
                    }
                    1 -> {
                        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Название или исполнитель") }, singleLine = true,
                            shape = RoundedCornerShape(18.dp))
                        SongList(songs.filter { it.first.contains(query, true) || it.second.contains(query, true) }, current, favorites,
                            onPlay = { current = it; playing = true })
                    }
                    2 -> SongList(songs.filter { favorites.contains(it.first) }, current, favorites,
                        onPlay = { current = it; playing = true })
                    3 -> {
                        Text("Плейлисты и загрузки появятся здесь", color = Color.Gray)
                        SongList(songs, current, favorites, onPlay = { current = it; playing = true })
                    }
                    4 -> {
                        Text("Оформление", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Светло-серый минимализм", color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text("Версия 0.1 — прототип интерфейса", color = Color.Gray)
                    }
                }
            }
        }
    }
}
@Composable
private fun AlbumTile() {
    Box(Modifier.size(42.dp).background(Color(0xFFC6C9CD), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = ink)
    }
}
@Composable
private fun SongList(list: List<Pair<String, String>>, current: Pair<String, String>, favorites: MutableList<String>, onPlay: (Pair<String, String>) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list) { song ->
            Surface(color = if (song == current) Color(0xFFE0E2E4) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onPlay(song) }.padding(8.dp)) {
                    AlbumTile()
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.first, fontWeight = FontWeight.Medium)
                        Text(song.second, fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { if (favorites.contains(song.first)) favorites.remove(song.first) else favorites.add(song.first) }) {
                        Icon(if (favorites.contains(song.first)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное", tint = ink)
                    }
                }
            }
        }
    }
}
