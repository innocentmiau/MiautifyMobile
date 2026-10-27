package io.github.innocentmiau.miautify.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import io.github.innocentmiau.miautify.data.MiautifyDatabase
import io.github.innocentmiau.miautify.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns the player, so music keeps playing with the screen off or the app closed. The media
 * session gives the notification, lock screen and headphone or Bluetooth buttons for free.
 * The app's screens talk to it through a MediaController (see [rememberPlayer]).
 *
 * The queue and position are saved on every change and loaded back, paused, when the
 * service starts again, so a phone that closed the app overnight continues where it was.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var queueStore: QueueStore

    // Writes the queue file in the background, one write at a time and in order.
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    // For the play history, which reads the player and so starts on the main thread.
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

        // Shuffle and repeat are kept apart from the queue, so they stay when the queue is cleared.
        val settings = Settings(this)
        player.shuffleModeEnabled = settings.shuffle
        player.repeatMode = settings.repeatMode

        queueStore = QueueStore(this)
        queueStore.restore(player)
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                // New queue, next song, play or pause, or a seek. Not while time just passes.
                if (events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY
                    )
                ) {
                    saveQueue(player)
                }
            }

            // Saved here and not in the app, so a change from the notification or a car counts too.
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                settings.shuffle = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                settings.repeatMode = repeatMode
            }
        })
        PlayTracker(player, MiautifyDatabase.get(this).plays(), mainScope)
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
            // Last save with the exact position. It goes through the same queue as the others,
            // so an older save still waiting can't land after it. Not cancelled: it must finish.
            saveQueue(player)
            player.release()
            release()
        }
        session = null
        // Stops a play count still waiting for its 30 seconds: the music has stopped.
        mainScope.cancel()
        super.onDestroy()
    }

    private fun saveQueue(player: Player) {
        // Read the player here on the main thread, then write the file in the background.
        val queue = queueStore.snapshot(player)
        saveScope.launch { queueStore.write(queue) }
    }
}
