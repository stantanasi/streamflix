package com.tanasi.streamflix.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.toCalendar
import java.util.Calendar

@Entity("episodes")
class Episode(
    @PrimaryKey
    var id: String,
    var number: Int,
    var title: String = "",
    released: String? = null,
    var poster: String? = null,

    var tvShow: TvShow? = null,
    var season: Season? = null,
) : WatchItem, AppAdapter.Item {

    constructor() : this("", 0)

    var released = released?.toCalendar()
    override var isWatched: Boolean = false
    override var watchedDate: Calendar? = null
    @Embedded
    override var watchHistory: WatchItem.WatchHistory? = null


    fun merge(episode: Episode) {
        this.isWatched = episode.isWatched
        this.watchedDate = episode.watchedDate
        this.watchHistory = episode.watchHistory
    }


    @Ignore
    override var itemType = AppAdapter.Type.EPISODE_ITEM
}