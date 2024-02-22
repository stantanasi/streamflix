package com.tanasi.streamflix.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.toCalendar

@Entity("episodes")
class Episode(
    @PrimaryKey
    var id: String,
    var number: Int,
    var title: String = "",
    released: String? = null,
    var poster: String? = null,
    var isWatched: Boolean = false,

    var tvShow: TvShow? = null,
    var season: Season? = null,
) : AppAdapter.Item {

    constructor() : this("", 0)

    var released = released?.toCalendar()


    @Ignore
    override var itemType = AppAdapter.Type.EPISODE_ITEM
}