package org.jellyfin.mobile.sessionbrowser

import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import org.jellyfin.mobile.sessionbrowser.page.AlbumLibraryPage
import org.jellyfin.mobile.sessionbrowser.page.AlbumsLibraryPage
import org.jellyfin.mobile.sessionbrowser.page.RootLibraryPage
import org.jellyfin.mobile.sessionbrowser.page.SearchLibraryPage
import org.jellyfin.mobile.sessionbrowser.page.UserViewLibraryPage
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.universalAudioApi
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder

class SessionBrowserCallback(
    private val api: ApiClient,
) : MediaLibrarySession.Callback {
    companion object {
        const val MAX_PAGE_SIZE = 1000
    }

    private val routes = mapOf(
        "root" to RootLibraryPage(api),
        "userView" to UserViewLibraryPage(api),
        "albums" to AlbumsLibraryPage(api),
        "album" to AlbumLibraryPage(api),
//        "artists" to ArtistsLibraryPage(api),
//        "artist" to ArtistLibraryPage(api),
//        "genres" to GenresLibraryPage(api),
//        "genre" to GenreLibraryPage(api),
//        "favorites" to FavoritesLibraryPage(api),
//        "playlists" to PlaylistsLibraryPage(api),
//        "playlist" to PlaylistLibraryPage(api),
//        "recents" to RecentsLibraryPage(api),
        "search" to SearchLibraryPage(api)
    )

    // TODO encode parameters with base64/urlencoding/whatever
    private fun serializeRouteId(
        path: String,
        parameters: List<String> = emptyList(),
    ): String = (listOf("jellyfin", path) + parameters).joinToString(
        separator = "/",
        transform = { URLEncoder.encode(it, Charsets.UTF_8.name()) },
    )

    private fun deserializeRouteId(id: String): Pair<String, List<String>>? {
        val parts = id.split("/")
            .map { URLDecoder.decode(it, Charsets.UTF_8.name()) }
        if (parts.size < 2 || parts[0] != "jellyfin") return null
        return parts[1] to parts.drop(2)
    }

//    private fun LibraryPageElement.BaseItem.toMediaItem(
//        groupTitle: String? = null,
//    ): MediaItem = MediaItem.Builder().apply {
//        val isAlbum = baseItem.albumId != null
//        val itemId = when {
//            baseItem.imageTags?.containsKey(ImageType.PRIMARY) == true -> baseItem.id
//            isAlbum -> baseItem.albumId
//            else -> null
//        }
//        val primaryImageUri = itemId?.let {
//            ImageProvider.buildItemUri(
//                itemId = itemId,
//                imageType = ImageType.PRIMARY,
//                imageTag = if (isAlbum) baseItem.albumPrimaryImageTag else baseItem.imageTags?.get(ImageType.PRIMARY),
//            )
//        }
//
//        setMediaId(baseItem.id.toString())
//        setMediaMetadata(
//            MediaMetadata.Builder().apply {
//                setTitle(baseItem.name ?: context.getString(R.string.media_service_car_item_no_title))
//                setIsBrowsable(false)
//                setIsPlayable(true)
//                setArtworkUri(primaryImageUri)
//                baseItem.album?.let(::setAlbumTitle)
//                baseItem.artists?.let { setArtist(it.joinToString()) }
//                baseItem.albumArtist?.let(::setAlbumArtist)
//                baseItem.indexNumber?.let(::setTrackNumber)
//                setExtras(
//                    bundleOf(
//                        androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE to groupTitle,
//                    ),
//                )
//            }.build(),
//        )
//    }.build()

    private fun LibraryPageElement.Item.toMediaItem(
        groupTitle: String? = null,
    ): MediaItem = MediaItem.Builder().apply {
        val extras = bundleOf()
        groupTitle?.let { extras.putString(androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, it) }

        if (action is LibraryItemAction.Navigate) {
            val contentStyle = when (action.grid) {
                true -> MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                false -> MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            }
            extras.putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, contentStyle)
            extras.putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, contentStyle)
            setMediaId(serializeRouteId(action.route, action.parameters))
        } else {
            setMediaId(id)
        }

        setMediaMetadata(
            MediaMetadata.Builder().apply {
                setTitle(title)
                setIsBrowsable(action is LibraryItemAction.Navigate)
                setIsPlayable(action is LibraryItemAction.Play)
                setArtworkUri(image)

                // TODO album info etc.
                // setArtist("test")
                setExtras(extras)
            }.build(),
        )
    }.build()

    private fun List<LibraryPageElement>.toMediaItems(): List<MediaItem> = flatMap { element ->
        when (element) {
            is LibraryPageElement.Group -> element.items.map { item -> item.toMediaItem(groupTitle = element.title) }
            is LibraryPageElement.Item -> listOf(element.toMediaItem())
        }
    }

    private fun createPageResult(
        routeId: String,
        params: LibraryParams? = null,
    ): LibraryResult<MediaItem> {
        val (route) = requireNotNull(deserializeRouteId(routeId))
        val page = routes[route]

        return if (page == null) {
            LibraryResult.ofError(LibraryResult.RESULT_ERROR_NOT_SUPPORTED, params)
        } else {
            LibraryResult.ofItem(
                MediaItem.Builder()
                    .setMediaId(routeId)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(true)
                            .build(),
                    )
                    .build(),
                LibraryParams.Builder().build(),
            )
        }
    }

    private suspend fun createPageContentResult(
        routeId: String,
        params: LibraryParams? = null,
        pageIndex: Int,
        pageSize: Int,
    ): LibraryResult<ImmutableList<MediaItem>> {
        val (route, parameters) = requireNotNull(deserializeRouteId(routeId))
        val page = routes[route]
        val items = page?.getContent(parameters, pageIndex * pageSize, minOf(pageSize, MAX_PAGE_SIZE))?.toMediaItems()

        return if (items == null) LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        else LibraryResult.ofItemList(items, params)
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> = CoroutineScope(Dispatchers.IO).future {
        val rootPageId = when {
            // TODO: recent and suggested pages not implemented yet
            params?.isRecent == true -> "recent"
            params?.isSuggested == true -> "suggested"
            else -> "root"
        }

        Timber.d("onGetLibraryRoot $session $browser $params $rootPageId")
        createPageResult(serializeRouteId(rootPageId), params)
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = CoroutineScope(Dispatchers.IO).future {
        Timber.d("onGetChildren $parentId $page $pageSize $params")
        createPageContentResult(parentId, params, page, pageSize)
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> = CoroutineScope(Dispatchers.IO).future {
        Timber.d("onSearch $query $params")

        // Add fake item count because we have not actually searched yet
        session.notifySearchResultChanged(browser, query, 1, params)
        LibraryResult.ofVoid(params)
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = CoroutineScope(Dispatchers.IO).future {
        Timber.d("onGetSearchResult $query $page $pageSize $params")
        createPageContentResult(serializeRouteId("search", listOf(query)), params, page, pageSize)
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = CoroutineScope(Dispatchers.IO).future {
        Timber.d("onGetItem $session $browser $mediaId")
        // TODO support returning items (baseitemdto)
        createPageResult(mediaId)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> = CoroutineScope(Dispatchers.IO).future {
        Timber.d("onAddMediaItems $mediaSession $controller $mediaItems")

        mediaItems.map {
            // TODO Use postedplaybackinfo api thing
            val playbackUri = api.universalAudioApi.getUniversalAudioStreamUrl(
                itemId = it.mediaId.toUUID(),
                deviceId = api.deviceInfo.id,
                maxStreamingBitrate = 140000000,
                container = listOf(
                    "opus",
                    "mp3|mp3",
                    "aac",
                    "m4a",
                    "m4b|aac",
                    "flac",
                    "webma",
                    "webm",
                    "wav",
                    "ogg",
                ),
                transcodingProtocol = MediaStreamProtocol.HLS,
                transcodingContainer = "ts",
                audioCodec = "aac",
                enableRemoteMedia = true,
            )

            it.buildUpon().setUri(playbackUri + "&ApiKey=${api.accessToken}").build()
        }
    }
}
