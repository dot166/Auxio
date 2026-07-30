/*
 * Copyright (c) 2020 Auxio Project
 * AudioPreview.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.android.music.preview

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.music.R
import org.oxycblt.auxio.playback.ui.StyledSeekBar
import org.oxycblt.auxio.playback.ui.button.ScaledPlaybackButton

/** Dialog that comes up in response to various music-related VIEW intents. */
class AudioPreview : ComponentActivity() {
    private lateinit var mTextLine1: TextView
    private lateinit var mTextLine2: TextView
    private lateinit var mSeekBar: StyledSeekBar
    private lateinit var player: ExoPlayer

    private var mUiPaused = true
    private var mDuration = 0L
    private var mUri: Uri? = null

    private val mProgressRefresher = Handler(Looper.getMainLooper())

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        val intent =
            intent
                ?: run {
                    finish()
                    return
                }

        mUri =
            intent.data
                ?: run {
                    finish()
                    return
                }

        volumeControlStream = AudioManager.STREAM_MUSIC
        setContentView(R.layout.audiopreview)

        mTextLine1 = findViewById(R.id.line1)
        mTextLine2 = findViewById(R.id.line2)
        mSeekBar = findViewById(R.id.playback_seek_bar)

        mTextLine1.isSelected = true
        mTextLine2.isSelected = true

        // --- Media3 player ---
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
        player =
            ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()

        player.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        showPostPrepareUI()
                    } else if (state == Player.STATE_ENDED) {
                        mSeekBar.positionDs = mSeekBar.durationDs
                        updatePlayPause()
                    }
                }

                override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                    val title = resolveTitle(this@AudioPreview, mUri!!, metadata.title)
                    mTextLine1.text = title

                    val artist = metadata.artist?.toString()
                    if (!artist.isNullOrBlank()) {
                        mTextLine2.text = artist
                        mTextLine2.visibility = View.VISIBLE
                    } else {
                        mTextLine2.visibility = View.GONE
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    finish()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPause()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mTextLine1.text = resolveTitle(this@AudioPreview, mUri!!, null)
                }
            }
        )

        val mediaItem = MediaItem.fromUri(mUri!!)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun onPause() {
        super.onPause()
        mUiPaused = true
        mProgressRefresher.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        mUiPaused = false
    }

    override fun onDestroy() {
        mProgressRefresher.removeCallbacksAndMessages(null)
        player.release()
        super.onDestroy()
    }

    private fun stopPlayback() {
        mProgressRefresher.removeCallbacksAndMessages(null)
        player.release()
    }

    override fun onUserLeaveHint() {
        stopPlayback()
        finish()
        super.onUserLeaveHint()
    }

    private fun showPostPrepareUI() {
        mDuration = (player.duration / 100).coerceAtLeast(0L)

        if (mDuration > 0) {
            mSeekBar.durationDs = mDuration
            if (!mSeekBar.isActivated) {
                mSeekBar.positionDs = player.currentPosition / 100
            }
        }

        mSeekBar.listener = object : StyledSeekBar.Listener {
            override fun onSeekConfirmed(positionDs: Long) {
                val target = positionDs.coerceIn(0, player.duration / 100)

                player.seekTo(target * 100)
            }
        }

        mProgressRefresher.removeCallbacksAndMessages(null)
        mProgressRefresher.postDelayed(ProgressRefresher(), 200)

        updatePlayPause()
    }

    private fun start() {
        player.play()
        mProgressRefresher.postDelayed(ProgressRefresher(), 200)
    }

    inner class ProgressRefresher : Runnable {
        @SuppressLint("DefaultLocale")
        override fun run() {
            if (!mSeekBar.isActivated && mDuration > 0) {
                val pos = player.currentPosition / 100
                val clamped = pos.coerceIn(0, mSeekBar.durationDs)
                mSeekBar.positionDs = clamped
            }

            mProgressRefresher.removeCallbacksAndMessages(null)
            if (!mUiPaused) {
                mProgressRefresher.postDelayed(this, 200)
            }
        }
    }

    private fun updatePlayPause() {
        findViewById<ScaledPlaybackButton>(R.id.playback_play_pause).isChecked = player.isPlaying
        mSeekBar.setWaveEnabled(player.isPlaying)
    }

    fun resolveTitle(context: Context, uri: Uri, metadataTitle: CharSequence?): String {
        metadataTitle?.toString()?.trim()?.let { if (it.isNotEmpty()) return it }

        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) return name
                }
            }

        uri.path?.substringAfterLast('/')?.let { if (it.isNotBlank()) return it }

        return uri.toString()
    }

    fun playPauseClicked(v: View?) {
        when {
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0)
                player.play()
            }
            player.isPlaying -> {
                player.pause()
            }
            else -> {
                player.play()
            }
        }
        updatePlayPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    start()
                }
                updatePlayPause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
                updatePlayPause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                }
                updatePlayPause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND -> return true
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                stopPlayback()
                finish()
                return true
            }
            else -> return super.onKeyDown(keyCode, event)
        }
    }
}
