package io.github.innocentmiau.miautify.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the player, so music keeps playing with the screen off or the app closed. The media
 * session gives the notification, lock screen and headphone or Bluetooth buttons for free.
 * The app's screens talk to it through a MediaController (see [rememberPlayer]).
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val music = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this)
            // true: pause for calls and other apps' audio, and resume after.
            .setAudioAttributes(music, true)
            // Pause when headphones are unplugged or Bluetooth disconnects.
            .setHandleAudioBecomingNoisy(true)
            // Keep the CPU awake while playing with the screen off. Files are local, so no Wi-Fi lock.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Swiping the app away keeps the music going, but ends the service if nothing is playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
