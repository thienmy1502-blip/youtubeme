package com.example.ytmusic

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {

    private var future: ListenableFuture<MediaController>? = null
    private var controller by mutableStateOf<MediaController?>(null)

    private val queue = mutableStateListOf<Track>()
    private var currentIndex by mutableIntStateOf(-1)
    private var isPlaying by mutableStateOf(false)

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@MainActivity.isPlaying = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentIndex = controller?.currentMediaItemIndex ?: -1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+: cần quyền thông báo để hiện điều khiển nhạc
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            MaterialTheme {
                AppScreen(
                    queue = queue,
                    currentIndex = currentIndex,
                    isPlaying = isPlaying,
                    ready = controller != null,
                    onAdd = ::addTrack,
                    onPlayIndex = ::playIndex,
                    onPrev = { controller?.seekToPrevious() },
                    onToggle = {
                        controller?.let { if (it.isPlaying) it.pause() else it.play() }
                    },
                    onNext = { controller?.seekToNext() },
                    onClear = ::clearQueue
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val f = MediaController.Builder(this, token).buildAsync()
        future = f
        f.addListener({
            val c = f.get()
            c.addListener(playerListener)
            controller = c
            syncFromController(c)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        controller?.removeListener(playerListener)
        future?.let { MediaController.releaseFuture(it) }
        controller = null
        future = null
        super.onStop()
    }

    /** Đồng bộ lại hàng đợi khi mở lại app (service có thể vẫn đang phát). */
    private fun syncFromController(c: MediaController) {
        queue.clear()
        for (i in 0 until c.mediaItemCount) {
            val item = c.getMediaItemAt(i)
            queue.add(
                Track(
                    title = item.mediaMetadata.title?.toString() ?: "Bài ${i + 1}",
                    url = item.localConfiguration?.uri?.toString() ?: ""
                )
            )
        }
        currentIndex = if (c.mediaItemCount > 0) c.currentMediaItemIndex else -1
        isPlaying = c.isPlaying
    }

    private fun addTrack(url: String, title: String) {
        val c = controller ?: return
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .build()
        c.addMediaItem(item)
        queue.add(Track(title, url))
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        if (c.mediaItemCount == 1) {
            currentIndex = 0
            c.play()
        }
    }

    private fun playIndex(index: Int) {
        val c = controller ?: return
        c.seekTo(index, 0L)
        c.play()
        currentIndex = index
    }

    private fun clearQueue() {
        controller?.apply {
            stop()
            clearMediaItems()
        }
        queue.clear()
        currentIndex = -1
    }
}
