package org.jellyfin.mobile.sessionbrowser

fun interface LibraryPage {
    suspend fun getContent(parameters: List<String>, offset: Int, limit: Int): List<LibraryPageElement>
}
