package com.tanasi.streamflix.models

sealed interface WatchItem {

    var watchHistory: WatchHistory?

    data class WatchHistory(
        val lastEngagementTimeUtcMillis: Long,
        val lastPlaybackPositionMillis: Long,
        val durationMillis: Long,
    )
}