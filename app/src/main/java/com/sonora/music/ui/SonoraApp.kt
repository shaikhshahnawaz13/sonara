package com.sonora.music.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.sonora.music.playback.PlaybackService
import com.sonora.music.data.LocalMusicRepository
import com.sonora.music.model.Track
import java.util.concurrent.TimeUnit

private enum class Tab(val label: String) {
    HOME("Home"), SEARCH("Search"), LIBRARY("Library")
}

@Composable
fun SonoraApp() {
    var selectedTab by remember { mutableStateOf(Tab.HOME) }
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val audioPermission = remember {
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    val controllerFuture = remember {
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        ).buildAsync()
    }
    DisposableEffect(controllerFuture) {
        controllerFuture.addListener(
            { runCatching { mediaController = controllerFuture.get() } },
            context.mainExecutor
        )
        onDispose {
            mediaController?.release()
        }
    }

    val visibleTracks = remember(tracks, query) {
        if (query.isBlank()) tracks else tracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true)
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            tracks = runCatching { LocalMusicRepository(context).getDeviceTracks() }
                .getOrDefault(emptyList())
        }
    }

    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        mediaController?.addListener(listener)
        onDispose { mediaController?.removeListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF17172A), Color(0xFF0B0C13), Color(0xFF08090E))
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SONORA", fontSize = 13.sp, letterSpacing = 4.sp,
                    color = Color(0xFFB8AEFF), fontWeight = FontWeight.Bold)
                Text(
                    when (selectedTab) {
                        Tab.HOME -> "Find your frequency."
                        Tab.SEARCH -> "Search your sound."
                        Tab.LIBRARY -> "Your collection."
                    },
                    fontSize = 27.sp, lineHeight = 32.sp,
                    color = Color(0xFFF5F2FF), fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                Modifier.size(42.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = .07f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.GraphicEq, null, tint = Color(0xFFB8AEFF))
            }
        }
        Spacer(Modifier.height(24.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                androidx.compose.animation.fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                    androidx.compose.animation.fadeOut(tween(120))
            },
            label = "tab-content"
        ) { tab ->
            when (tab) {
                Tab.HOME -> HomeContent(
                    trackCount = tracks.size,
                    permissionGranted = permissionGranted,
                    onRequestLibrary = { permissionLauncher.launch(audioPermission) },
                    tracks = tracks.take(5),
                    onTrackClick = {
                        currentTrack = it
                        mediaController?.apply {
                            setMediaItem(MediaItem.fromUri(it.uri))
                            prepare()
                            play()
                        }
                    }
                )
                Tab.SEARCH -> SearchContent(
                    query = query,
                    onQueryChange = { query = it },
                    tracks = visibleTracks,
                    permissionGranted = permissionGranted,
                    onTrackClick = {
                        currentTrack = it
                        mediaController?.apply {
                            setMediaItem(MediaItem.fromUri(it.uri))
                            prepare()
                            play()
                        }
                    }
                )
                Tab.LIBRARY -> LibraryContent(
                    tracks = tracks,
                    permissionGranted = permissionGranted,
                    onRequestLibrary = { permissionLauncher.launch(audioPermission) },
                    onTrackClick = {
                        currentTrack = it
                        mediaController?.apply {
                            setMediaItem(MediaItem.fromUri(it.uri))
                            prepare()
                            play()
                        }
                    }
                )
            }
        }

        Spacer(Modifier.weight(1f))
        currentTrack?.let { track ->
            MiniPlayer(
                track = track,
                isPlaying = isPlaying,
                onToggle = {
                    mediaController?.let { player ->
                        if (player.isPlaying) player.pause() else player.play()
                    }
                },
                onClose = { currentTrack = null }
            )
            Spacer(Modifier.height(10.dp))
        }
        NavigationBar(
            containerColor = Color(0xCC12131D),
            contentColor = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
        ) {
            Tab.values().forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = {
                        Icon(
                            when (tab) {
                                Tab.HOME -> Icons.Rounded.Home
                                Tab.SEARCH -> Icons.Rounded.Search
                                Tab.LIBRARY -> Icons.Rounded.LibraryMusic
                            }, contentDescription = tab.label
                        )
                    },
                    label = { Text(tab.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFB8AEFF),
                        selectedTextColor = Color(0xFFB8AEFF),
                        indicatorColor = Color(0xFF51477C).copy(alpha = .35f),
                        unselectedIconColor = Color(0xFF9292A2),
                        unselectedTextColor = Color(0xFF9292A2)
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    trackCount: Int,
    permissionGranted: Boolean,
    onRequestLibrary: () -> Unit,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().height(190.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF51447C), Color(0xFF262B50), Color(0xFF151A2B))
                        )
                    ).padding(22.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text("A QUIETER KIND OF LOUD", color = Color(0xFFD8D0FF),
                        fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Make room\nfor music.", color = Color.White,
                        fontSize = 29.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (permissionGranted) "$trackCount tracks found on this device"
                        else "Your library, beautifully organized",
                        color = Color(0xFFD4D0E5), fontSize = 12.sp
                    )
                }
                Icon(
                    Icons.Rounded.GraphicEq, null,
                    modifier = Modifier.align(Alignment.BottomEnd).size(76.dp),
                    tint = Color.White.copy(alpha = .16f)
                )
            }
        }
        item { SectionTitle("On this device", "LOCAL LIBRARY") }
        if (!permissionGranted) {
            item {
                GlassPanel {
                    Text("Start with the music you already own.",
                        color = Color.White, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text("Allow audio access to scan your device. Nothing is uploaded.",
                        color = Color(0xFFAAA9B8), fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onRequestLibrary) {
                        Text("Show my music")
                    }
                }
            }
        } else if (tracks.isEmpty()) {
            item {
                GlassPanel {
                    Text("Your library is quiet for now.", color = Color.White)
                    Text("Add audio files to your device and refresh the library.",
                        color = Color(0xFFAAA9B8), fontSize = 12.sp)
                }
            }
        } else {
            items(tracks, key = { it.id }) { track ->
                TrackRow(track = track, onClick = { onTrackClick(track) })
            }
        }
    }
}

@Composable
private fun SearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    tracks: List<Track>,
    permissionGranted: Boolean,
    onTrackClick: (Track) -> Unit
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Songs, artists, albums") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF9F91F5),
                unfocusedBorderColor = Color.White.copy(alpha = .12f),
                focusedContainerColor = Color.White.copy(alpha = .06f),
                unfocusedContainerColor = Color.White.copy(alpha = .04f)
            )
        )
        Spacer(Modifier.height(18.dp))
        if (!permissionGranted) {
            Text("Your device library will appear here after you grant audio access.",
                color = Color(0xFFAAA9B8), fontSize = 13.sp)
        } else if (tracks.isEmpty()) {
            Text("No matches. Try another search.", color = Color(0xFFAAA9B8))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(track, onClick = { onTrackClick(track) })
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(
    tracks: List<Track>,
    permissionGranted: Boolean,
    onRequestLibrary: () -> Unit,
    onTrackClick: (Track) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LibraryMusic, null, tint = Color(0xFFB8AEFF))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Device library", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("${tracks.size} tracks", color = Color(0xFFAAA9B8), fontSize = 12.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFAAA9B8))
            }
        }
        if (!permissionGranted) {
            Button(onClick = onRequestLibrary) { Text("Scan device music") }
        } else if (tracks.isEmpty()) {
            Text("No local music found.", color = Color(0xFFAAA9B8))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(track, onClick = { onTrackClick(track) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, eyebrow: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(eyebrow, color = Color(0xFF9B91D8), fontSize = 9.sp, letterSpacing = 2.sp)
        Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GlassPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = .055f))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = .035f))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF51447C), Color(0xFF25364D)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = Color(0xFFE3DEFF))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Color(0xFFAAA9B8), fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(track.durationMs), color = Color(0xFF8F8E9D), fontSize = 11.sp)
    }
}

@Composable
private fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF29263D).copy(alpha = .96f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = Color(0xFFC5BAFF))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, maxLines = 1,
                overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(track.artist, color = Color(0xFFB9B5C9), fontSize = 11.sp)
        }
        IconButton(onClick = onToggle) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (isPlaying) "Pause" else "Play",
                tint = Color.White
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, "Dismiss player", tint = Color.White)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(minutes, seconds)
}
