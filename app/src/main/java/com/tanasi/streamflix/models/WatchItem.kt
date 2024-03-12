package com.tanasi.streamflix.models

sealed interface WatchItem {

    var isWatched: Boolean
    var watchHistory: WatchHistory?

    data class WatchHistory(
        val lastEngagementTimeUtcMillis: Long,
        val lastPlaybackPositionMillis: Long,
        val durationMillis: Long,
    )
}