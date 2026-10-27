package com.example.ytmusic

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

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

private const val YT_HOME = "https://m.youtube.com/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeTab() {
    var input by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf(YT_HOME) }

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Dán link YouTube hoặc gõ từ khoá tìm kiếm") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            modifier = Modifier.padding(vertical = 8.dp),
            onClick = {
                val id = extractVideoId(input)
                target = when {
                    id != null -> "https://m.youtube.com/watch?v=$id"
                    input.isNotBlank() -> "https://m.youtube.com/results?search_query=" + Uri.encode(input.trim())
                    else -> YT_HOME
                }
            }
        ) { Text("Phát / Tìm") }

        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.userAgentString = settings.userAgentString.replace("; wv", "")
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                            // Chỉ mở link http(s) trong WebView, chặn intent:// mở app khác
                            return !request.url.scheme.orEmpty().startsWith("http")
                        }
                    }
                }
            },
            update = { wv ->
                if (wv.tag != target) {
                    wv.tag = target
                    wv.loadUrl(target)
                }
            },
            onRelease = { it.destroy() }
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
