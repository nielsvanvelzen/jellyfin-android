package org.jellyfin.mobile.sessionbrowser.page

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.sessionbrowser.LibraryPage
import org.jellyfin.mobile.sessionbrowser.LibraryPageElement
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType

class SearchLibraryPage(
    private val api: ApiClient,
) : LibraryPage {
    private suspend fun search(query: String, itemTypes: Collection<BaseItemKind>) = withContext(Dispatchers.IO) {
        async {
            api.itemsApi.getItems(
                searchTerm = query,
                imageTypeLimit = 1,
                enableImageTypes = setOf(ImageType.PRIMARY),
                limit = 50,
                includeItemTypes = itemTypes,
            )
        }
    }

    override suspend fun getContent(parameters: List<String>, offset: Int, limit: Int): List<LibraryPageElement> {
        val query = parameters.first()
        if (query.isBlank()) return emptyList()

        val (playlists, albums, artists) = listOf(
            search(query, setOf(BaseItemKind.PLAYLIST)),
            search(query, setOf(BaseItemKind.MUSIC_ALBUM)),
            search(query, setOf(BaseItemKind.MUSIC_ARTIST)),
        )
            .awaitAll()
            .map { response ->
                response.content.items.map { item ->
                    LibraryPageElement.baseItem(api, item)
                }
            }

        return listOf(
            LibraryPageElement.Group("Playlists", playlists),
            LibraryPageElement.Group("Albums", albums),
            LibraryPageElement.Group("Artists", artists),
        )
    }
}
