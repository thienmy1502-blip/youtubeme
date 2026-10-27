package com.example.ytmusic

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

data class Track(val title: String, val url: String)

/** Lấy ID video 11 ký tự từ link YouTube (watch, youtu.be, shorts, embed...) hoặc từ chính ID. */
fun extractVideoId(input: String): String? {
    val s = input.trim()
    if (Regex("^[A-Za-z0-9_-]{11}$").matches(s)) return s
    return Regex("(?:v=|youtu\\.be/|embed/|shorts/|live/)([A-Za-z0-9_-]{11})")
        .find(s)?.groupValues?.get(1)
}

@Composable
fun AppScreen(
    queue: List<Track>,
    currentIndex: Int,
    isPlaying: Boolean,
    ready: Boolean,
    onAdd: (url: String, title: String) -> Unit,
    onPlayIndex: (Int) -> Unit,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onClear: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Xem YouTube") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Nghe nền") })
            }
            if (tab == 0) {
                YouTubeTab()
            } else {
                BackgroundTab(queue, currentIndex, isPlaying, ready, onAdd, onPlayIndex, onPrev, onToggle, onNext, onClear)
            }
        }
    }
}

@Composable
private fun YouTubeTab() {
    var input by rememberSaveable { mutableStateOf("") }
    var videoId by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Link hoặc ID video YouTube") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            val id = extractVideoId(input)
            if (id == null) {
                error = "Link không hợp lệ"
            } else {
                error = null
                videoId = id
                player?.loadVideo(id, 0f)
            }
        }) { Text("Phát video") }

        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }

        AndroidView(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)
                    addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            player = youTubePlayer
                            videoId?.let { youTubePlayer.loadVideo(it, 0f) }
                        }
                    })
                }
            },
            onRelease = {
                lifecycleOwner.lifecycle.removeObserver(it)
                it.release()
                player = null
            }
        )

        Text(
            "Lưu ý: trình phát YouTube chính thức sẽ dừng khi tắt màn hình. " +
                "Muốn nghe nền, dùng tab \"Nghe nền\".",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun BackgroundTab(
    queue: List<Track>,
    currentIndex: Int,
    isPlaying: Boolean,
    ready: Boolean,
    onAdd: (String, String) -> Unit,
    onPlayIndex: (Int) -> Unit,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onClear: () -> Unit
) {
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL âm thanh trực tiếp (mp3, m4a, m3u8...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Tên bài (tuỳ chọn)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = ready && url.isNotBlank(),
                onClick = {
                    val u = url.trim()
                    val t = title.trim().ifEmpty {
                        Uri.parse(u).lastPathSegment ?: "Bài ${queue.size + 1}"
                    }
                    onAdd(u, t)
                    url = ""
                    title = ""
                }
            ) { Text("Thêm vào hàng đợi") }
            OutlinedButton(enabled = queue.isNotEmpty(), onClick = onClear) { Text("Xoá hết") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(enabled = ready, onClick = onPrev) { Text("⏮ Trước") }
            Button(enabled = ready && queue.isNotEmpty(), onClick = onToggle) {
                Text(if (isPlaying) "⏸ Tạm dừng" else "▶ Phát")
            }
            OutlinedButton(enabled = ready, onClick = onNext) { Text("Sau ⏭") }
        }

        HorizontalDivider()

        if (queue.isEmpty()) {
            Text("Hàng đợi trống. Dán URL âm thanh và bấm \"Thêm vào hàng đợi\".")
        }
        LazyColumn {
            itemsIndexed(queue) { index, track ->
                Text(
                    text = (if (index == currentIndex) "▶ " else "    ") + track.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayIndex(index) }
                        .padding(vertical = 12.dp)
                )
                HorizontalDivider()
            }
        }
    }
}
