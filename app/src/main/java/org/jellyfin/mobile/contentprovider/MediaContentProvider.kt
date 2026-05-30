package org.jellyfin.mobile.contentprovider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import org.jellyfin.mobile.R
import java.io.File
import java.io.FileNotFoundException

/**
 * TODO: Interactive content provider connected to the current Jellyfin server to allow streaming etc. via external apps.
 */

class MediaContentProvider : DocumentsProvider() {
    companion object {
        const val ROOT_ID = "root"
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(
            projection ?: arrayOf(
                DocumentsContract.Root.COLUMN_ROOT_ID,
                DocumentsContract.Root.COLUMN_DOCUMENT_ID,
                DocumentsContract.Root.COLUMN_TITLE,
                DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.COLUMN_ICON,
            )
        )

        val flags = DocumentsContract.Root.FLAG_LOCAL_ONLY

        cursor.addRow(
            arrayOf<Any>(
                ROOT_ID,
                ROOT_ID,
                context?.getString(R.string.app_name).orEmpty(),
                flags,
                R.mipmap.ic_launcher
            )
        )
        return cursor
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(
            projection ?: arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_FLAGS,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
        )

        val file = getFileForDocId(documentId)
        if (!file.exists()) return cursor

        val mimeType =
            if (file.isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension) ?: "application/octet-stream"
        val flags = if (file.isDirectory) DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE else 0

        cursor.addRow(arrayOf(documentId, file.name, flags, file.length(), mimeType))
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String, projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(
            projection ?: arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_FLAGS,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
        )

        val parentFile = getFileForDocId(parentDocumentId)
        if (parentFile.exists() && parentFile.isDirectory) {
            for (file in parentFile.listFiles() ?: emptyArray()) {
                val docId = file.relativeTo(context!!.filesDir).path
                val mimeType =
                    if (file.isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else android.webkit.MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(file.extension) ?: "application/octet-stream"
                val flags = 0
                cursor.addRow(arrayOf(docId, file.name, flags, file.length(), mimeType))
            }
        }

        return cursor
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Read-only provider")
        val file = getFileForDocId(documentId)
        if (!file.exists()) throw FileNotFoundException("File not found: $documentId")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun getFileForDocId(documentId: String): File {
        return if (documentId == ROOT_ID) context!!.filesDir
        else File(context!!.filesDir, documentId)
    }
}
