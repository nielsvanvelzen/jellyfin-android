package org.jellyfin.mobile.sessionbrowser.page

import org.jellyfin.mobile.sessionbrowser.LibraryItemAction
import org.jellyfin.mobile.sessionbrowser.LibraryPage
import org.jellyfin.mobile.sessionbrowser.LibraryPageElement
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.CollectionType

/**
 * Root library page that returns the available libraries (user views) for Android Auto playback.
 * Note that this should ideally not exceed 4 items.
 */
class RootLibraryPage(
    private val api: ApiClient,
) : LibraryPage {
    override suspend fun getContent(parameters: List<String>, offset: Int, limit: Int): List<LibraryPageElement> {
        val userViews by api.userViewsApi.getUserViews()
        return userViews.items
            .filter { it.collectionType == CollectionType.MUSIC }
            .map {
                LibraryPageElement.baseItem(
                    api = api,
                    item = it,
                    id = "userView,${it.id}",
                    image = null,
                    action = LibraryItemAction.Navigate("userView", listOf(it.id.toString()), grid = false),
                )
            }
    }
}

