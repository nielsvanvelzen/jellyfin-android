package org.jellyfin.mobile.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import org.jellyfin.mobile.R
import java.io.File

class StorageManager(
    private val context: Context,
    private val appPreferences: AppPreferences,
) {
    private val defaultStorageLocation get() = (Environment.getExternalStorageDirectory().absolutePath + File.separator + context.getString(R.string.app_name_short))

    fun getStorageLocation(): DocumentFile = appPreferences.storageLocation?.toUri()?.let { DocumentFile.fromTreeUri(context, it) }
        ?: DocumentFile.fromFile(File(defaultStorageLocation))

    fun changeStorageLocation(location: Uri) {
        if (appPreferences.storageLocation?.toUri() == location) return

        context.contentResolver.takePersistableUriPermission(location, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        appPreferences.storageLocation = location.toString()
    }
}
