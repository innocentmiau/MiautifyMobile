package io.github.innocentmiau.miautify.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Collections
import kotlin.math.roundToInt

/** A cover as a screen shows it: still loading, known to have no picture, or the picture. */
sealed interface Cover {
    data object Loading : Cover

    data object None : Cover

    data class Loaded(val image: ImageBitmap) : Cover
}

/**
 * Song covers, read from the picture embedded in the mp3, or else from the folder's cover
 * image (like cover.jpg). Each one is read once, shrunk to a small thumbnail and kept on disk
 * (the app's cache folder) and in memory, so scrolling back and later launches don't read the
 * file again.
 */
object Covers {
    /**
     * Thumbnail size in pixels: sharp for an album card in the browse grid (about 110 dp wide)
     * on the sharpest screens, and more than enough for a 48 dp list cover.
     */
    private const val SIZE = 320

    // About 1/16 of the app's memory for thumbnails in use, the least recently shown dropped first.
    private val memory = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 16).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    // Keys looked up already that have no picture, so scrolling back shows the music note at
    // once instead of loading again. Tiny (one short text per song), so never trimmed.
    private val missing: MutableSet<String> = Collections.synchronizedSet(HashSet())

    // At most two files read at once, so a fast scroll doesn't flood the storage.
    private val loading = Dispatchers.IO.limitedParallelism(2)

    /**
     * The cover of [file] if it is known already, without waiting: in memory, or known to have
     * none. Null when it must still be loaded. Cheap, since every row on screen asks.
     */
    fun cached(file: SongFile): Cover? {
        val songKey = songKey(file)
        memory.get(songKey)?.let { return Cover.Loaded(it) }
        // Without its own picture, the folder's image is next.
        if (songKey !in missing) return null
        val coverId = file.folderCoverId ?: return Cover.None
        val folderKey = folderKey(coverId)
        memory.get(folderKey)?.let { return Cover.Loaded(it) }
        return if (folderKey in missing) Cover.None else null
    }

    /**
     * The thumbnail of [file]: its embedded picture, or else its folder's cover image (like
     * cover.jpg), or null when it has neither.
     */
    suspend fun load(context: Context, folder: Uri, file: SongFile): Cover = withContext(loading) {
        val image = loadImage(context, folder, file)
        if (image != null) Cover.Loaded(image) else Cover.None
    }

    private fun loadImage(context: Context, folder: Uri, file: SongFile): ImageBitmap? {
        val dir = File(context.cacheDir, "covers").apply { mkdirs() }
        val embedded = thumbnailOf(dir, songKey(file)) {
            readEmbedded(context, MusicFolder.fileUri(folder, file))
        }
        if (embedded != null) return embedded

        // Shared by every song of the folder, so it is read and shrunk only once.
        val coverId = file.folderCoverId ?: return null
        return thumbnailOf(dir, folderKey(coverId)) {
            readFile(context, MusicFolder.documentUri(folder, coverId))
        }
    }

    /** The thumbnail saved as [key], from memory, then disk, then made from what [read] gives. */
    private fun thumbnailOf(dir: File, key: String, read: () -> ByteArray?): ImageBitmap? {
        memory.get(key)?.let { return it }
        // The file name is a hash of the key: the key has slashes and colons a name can't have.
        val saved = File(dir, "${sha1(key)}.jpg")
        val bitmap = if (saved.exists()) {
            // An empty file means "looked before, no picture".
            if (saved.length() == 0L) null else BitmapFactory.decodeFile(saved.path)
        } else {
            read()?.let(::thumbnail).also { write(it, saved) }
        }
        if (bitmap == null) missing += key
        return bitmap?.asImageBitmap()?.also { memory.put(key, it) }
    }

    /** Decodes [bytes] as a thumbnail whose shorter side is [SIZE] pixels, or null if it isn't an image. */
    fun thumbnail(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, SIZE) }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val scale = SIZE.toFloat() / minOf(decoded.width, decoded.height)
        if (scale >= 1f) return decoded
        return decoded.scale((decoded.width * scale).roundToInt(), (decoded.height * scale).roundToInt())
    }

    private fun readFile(context: Context, file: Uri): ByteArray? =
        runCatching { context.contentResolver.openInputStream(file)?.use { it.readBytes() } }.getOrNull()

    private fun readEmbedded(context: Context, file: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, file)
            retriever.embeddedPicture
        } catch (_: RuntimeException) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Saves [thumbnail], or an empty file when there is none. Written beside and renamed, so a half written file never shows. */
    private fun write(thumbnail: Bitmap?, file: File) {
        val temp = File(file.path + ".tmp")
        runCatching {
            temp.outputStream().use { thumbnail?.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            temp.renameTo(file)
        }
    }

    /** Changes when the song file changes, so an edited embedded cover is read again. */
    // SIZE is part of the keys, so changing it makes new thumbnails instead of reusing old ones.
    // Plain text, hashed only for the file name on disk: hashing for every row on screen was
    // work the main thread did while scrolling.
    private fun songKey(file: SongFile): String = "song|$SIZE|${file.documentId}|${file.size}|${file.lastModified}"

    /** One per folder image. A cover.jpg replaced under the same name keeps its old thumbnail until the cache is cleared. */
    private fun folderKey(coverId: String): String = "folder|$SIZE|$coverId"

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}

/**
 * The biggest power of two to divide the image size by while decoding, keeping the shorter side
 * at least [target] pixels. Decoding a fraction of the pixels saves memory and time.
 */
fun sampleSize(width: Int, height: Int, target: Int): Int {
    var sample = 1
    while (minOf(width, height) / (sample * 2) >= target) {
        sample *= 2
    }
    return sample
}
