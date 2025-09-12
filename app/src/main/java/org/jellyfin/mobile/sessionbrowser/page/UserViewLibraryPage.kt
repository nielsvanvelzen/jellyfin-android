package org.jellyfin.mobile.sessionbrowser.page

import org.jellyfin.mobile.sessionbrowser.LibraryItemAction
import org.jellyfin.mobile.sessionbrowser.LibraryPage
import org.jellyfin.mobile.sessionbrowser.LibraryPageElement
import org.jellyfin.sdk.api.client.ApiClient

class UserViewLibraryPage(
    private val api: ApiClient,
) : LibraryPage {
    override suspend fun getContent(parameters: List<String>, offset: Int, limit: Int): List<LibraryPageElement> {
        val itemId = parameters.first()

        return listOf(
            LibraryPageElement.Item(
                id = "albums",
                title = "Albums",
                action = LibraryItemAction.Navigate("albums", listOf(itemId)),
            ),

            LibraryPageElement.Item(
                id = "artists",
                title = "Artists",
                action = LibraryItemAction.Navigate("artists", listOf(itemId)),
            ),

            LibraryPageElement.Item(
                id = "favorites",
                title = "Favorites",
                action = LibraryItemAction.Navigate("favorites", listOf(itemId)),
            ),

            LibraryPageElement.Item(
                id = "genres",
                title = "Genres",
                action = LibraryItemAction.Navigate("genres", listOf(itemId)),
            ),

            LibraryPageElement.Item(
                id = "playlists",
                title = "Playlists",
                action = LibraryItemAction.Navigate("playlists", listOf(itemId)),
            ),

            LibraryPageElement.Item(
                id = "recent",
                title = "Recently played",
                action = LibraryItemAction.Navigate("recent", listOf(itemId)),
            ),
        )
    }
}
