package io.github.innocentmiau.miautify.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Access to the music folder the user picked in the system folder picker (Storage Access
 * Framework). The app can only read that folder, and only while it holds the permission.
 */
object MusicFolder {
    private const val READ = Intent.FLAG_GRANT_READ_URI_PERMISSION

    /** Keeps read access to [folder] after the app restarts, and gives back access to [previous]. */
    fun keepAccess(context: Context, folder: Uri, previous: Uri?) {
        val resolver = context.contentResolver
        if (previous != null && previous != folder) {
            // Throws if the permission is already gone, which is fine: nothing to give back.
            runCatching { resolver.releasePersistableUriPermission(previous, READ) }
        }
        resolver.takePersistableUriPermission(folder, READ)
    }

    /** False when the permission was removed, for example by clearing the app's data. */
    fun hasAccess(context: Context, folder: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri == folder && it.isReadPermission }

    /** The folder's name, or null when it can't be read (moved, deleted, or the SD card is out). */
    fun displayName(context: Context, folder: Uri): String? = runCatching {
        val document = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
        val columns = arrayOf(Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(document, columns, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    /** The picked folder a file URI from [fileUri] belongs to, to check that access to it is still held. */
    fun folderOf(file: Uri): Uri =
        DocumentsContract.buildTreeDocumentUri(file.authority, DocumentsContract.getTreeDocumentId(file))

    /** The URI that opens [file], a file found by [scan] in [folder]. */
    fun fileUri(folder: Uri, file: SongFile): Uri =
        DocumentsContract.buildDocumentUriUsingTree(folder, file.documentId)

    /**
     * Lists every mp3 in [folder] and its subfolders. Runs on a background thread, and stops
     * early when the caller no longer needs the result. Throws when a folder can't be read.
     */
    suspend fun scan(context: Context, folder: Uri): List<SongFile> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)

        findSongs(DocumentsContract.getTreeDocumentId(folder)) { folderId ->
            // Throws CancellationException if the scan was cancelled, for example by picking another folder.
            ensureActive()
            // One query returns everything directly inside the folder, much faster than
            // DocumentFile.listFiles(), which asks the system again for each file.
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(folder, folderId)
            resolver.query(children, columns, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            DocumentEntry(
                                id = cursor.getString(0),
                                name = cursor.getString(1).orEmpty(),
                                isFolder = cursor.getString(2) == Document.MIME_TYPE_DIR
                            )
                        )
                    }
                }
            } ?: throw IOException("Could not list $children")
        }
    }
}
