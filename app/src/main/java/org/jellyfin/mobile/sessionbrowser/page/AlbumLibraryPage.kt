package org.jellyfin.mobile.sessionbrowser.page

import org.jellyfin.mobile.sessionbrowser.LibraryPage
import org.jellyfin.mobile.sessionbrowser.LibraryPageElement
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.serializer.toUUID

class AlbumLibraryPage(
    private val api: ApiClient,
) : LibraryPage {
    override suspend fun getContent(parameters: List<String>, offset: Int, limit: Int): List<LibraryPageElement> {
        val albumId = parameters.first().toUUID()
        val result by api.itemsApi.getItems(
            parentId = albumId,
            sortBy = listOf(ItemSortBy.SORT_NAME),
            imageTypeLimit = 1,
            enableImageTypes = listOf(ImageType.PRIMARY),
            startIndex = offset,
            limit = limit,
        )
        return result.items.map { LibraryPageElement.baseItem(api, it) }
    }
}
