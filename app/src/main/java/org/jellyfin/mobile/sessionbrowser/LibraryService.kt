package org.jellyfin.mobile.sessionbrowser

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import org.koin.android.ext.android.get

class LibraryService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null
    private var callback: MediaLibrarySession.Callback? = null

    private val playerAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener: Player.Listener = object : Player.Listener {
    }

    private val exoPlayer: Player by lazy {
        ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(playerAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaLibrarySession? = mediaLibrarySession

    override fun onCreate() {
        super.onCreate()

        if (callback == null) callback = SessionBrowserCallback(get())
        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer, callback!!)
            .build()
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }

        super.onDestroy()
    }
}
