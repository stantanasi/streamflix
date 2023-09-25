package com.tanasi.streamflix.extractors

import com.tanasi.streamflix.models.Video

abstract class Extractor {

    abstract val name: String
    abstract val mainUrl: String

    abstract suspend fun extract(link: String): Video
}