package com.tanasi.streamflix.extractors

import com.tanasi.streamflix.models.Video

abstract class Extractor {

    abstract val name: String
    abstract val mainUrl: String
    open val aliasUrls: List<String> = emptyList()

    // THIS is the main method all subclasses must implement
    abstract suspend fun extract(link: String): Video

    // THIS is a convenience helper
    open suspend fun extract(link: String, server: Video.Server? = null): Video {
        return extract(link)
    }

    companion object {
        private val extractors = listOf(
            RabbitstreamExtractor(),
            RabbitstreamExtractor.MegacloudExtractor(),
            RabbitstreamExtractor.DokicloudExtractor(),
            RabbitstreamExtractor.PremiumEmbedingExtractor(),
            StreamhubExtractor(),
            VoeExtractor(),
            StreamtapeExtractor(),
            VidozaExtractor(),
            VidsrcToExtractor(),
            VidplayExtractor(),
            FilemoonExtractor(),
            VidplayExtractor.MyCloud(),
            VidplayExtractor.VidplayOnline(),
            MyFileStorageExtractor(),
            MoflixExtractor(),
            MStreamDayExtractor(),
            MStreamClickExtractor(),
            VidsrcNetExtractor(),
            StreamWishExtractor(),
            StreamWishExtractor.UqloadsXyz(),
            StreamWishExtractor.SwishExtractor(),
            StreamWishExtractor.HlswishExtractor(),
            StreamWishExtractor.PlayerwishExtractor(),
            StreamWishExtractor.SwiftPlayersExtractor(),
            TwoEmbedExtractor(),
            ChillxExtractor(),
            ChillxExtractor.JeanExtractor(),
            MoviesapiExtractor(),
            CloseloadExtractor(),
            LuluVdoExtractor(),
            DoodLaExtractor(),
            DoodLaExtractor.DoodLiExtractor(),
            VidPlyExtractor(),
            MagaSavorExtractor(),
            VidMoLyExtractor(),
            VidMoLyExtractor.ToDomain(),
            VideoSibNetExtractor(),
            SaveFilesExtractor(),
            BigWarpExtractor(),
            DoodLaExtractor.DoodExtractor(),
            LoadXExtractor(),
            VidHideExtractor(),
            VeevExtractor(),
            RidooExtractor(),
            USTRExtractor(),
        )

        suspend fun extract(link: String, server: Video.Server? = null): Video {
            val urlRegex = Regex("^(https?://)?(www\\.)?")
            val compareUrl = link.lowercase().replace(urlRegex, "")

            for (extractor in extractors) {
                if (compareUrl.startsWith(extractor.mainUrl.replace(urlRegex, ""))) {
                    return extractor.extract(link)
                } else {
                    for (aliasUrl in extractor.aliasUrls) {
                        if (compareUrl.startsWith(aliasUrl.lowercase().replace(urlRegex, ""))) {
                            return extractor.extract(link)
                        }
                    }
                }
            }
            for (extractor in extractors) {
                if (compareUrl.startsWith(
                        extractor.mainUrl.replace(
                            Regex("^(https?://)?(www\\.)?(.*?)(\\.[a-z]+)"),
                            "$3"
                        )
                    )
                ) {
                    return extractor.extract(link)
                } else {
                    for (aliasUrl in extractor.aliasUrls) {
                        if (compareUrl.startsWith(
                                aliasUrl.replace(
                                    Regex("^(https?://)?(www\\.)?(.*?)(\\.[a-z]+)"),
                                    "$3"
                                )
                            )
                        ) {
                            return extractor.extract(link)
                        }
                    }
                }
            }

            for (extractor in extractors){
                if ((server?.name?.lowercase() ?: "").contains(extractor.name.lowercase())){
                    return extractor.extract(link)
                }
            }
            throw Exception("No extractors found")
        }
    }
}