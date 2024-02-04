package com.tanasi.streamflix.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.tanasi.streamflix.adapters.AppAdapter

@Entity("seasons")
class Season(
    @PrimaryKey
    var id: String,
    var number: Int,
    var title: String = "",
    var poster: String = "",

    var tvShow: TvShow? = null,
    @Ignore
    var episodes: List<Episode> = listOf(),
) : AppAdapter.Item {

    constructor() : this("", 0)


    @Ignore
    override var itemType = AppAdapter.Type.SEASON_ITEM
}