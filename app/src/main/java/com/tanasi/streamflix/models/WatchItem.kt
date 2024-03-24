package com.tanasi.streamflix.models

import java.util.Calendar

sealed interface WatchItem {

    var isWatched: Boolean
    var watchedDate: Calendar?
    var watchHistory: WatchHistory?

    data class WatchHistory(
        val lastEngagementTimeUtcMillis: Long,
        val lastPlaybackPositionMillis: Long,
        val durationMillis: Long,
    )
}