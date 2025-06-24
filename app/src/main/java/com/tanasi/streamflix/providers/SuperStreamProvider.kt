package com.tanasi.streamflix.providers

import android.util.Base64
import com.google.gson.annotations.SerializedName
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SuperStreamProvider : Provider {

    override val name = "SuperStream"
    override val logo = ""
    override val language = "en"
    val url = Base64.decode(
        "aHR0cHM6Ly9zaG93Ym94LnNoZWd1Lm5ldA==",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8) + Base64.decode(
        "L2FwaS9hcGlfY2xpZW50L2luZGV4Lw==",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    override val baseUrl = url

    private val service = SuperStreamApiService.build()

    // We do not want content scanners to notice this scraping going on so we've hidden all constants
    // The source has its origins in China so I added some extra security with banned words
    // Mayhaps a tiny bit unethical, but this source is just too good :)
    // If you are copying this code please use precautions so they do not change their api.
    private val iv = Base64.decode(
        "d0VpcGhUbiE=",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    private val key = Base64.decode(
        "MTIzZDZjZWRmNjI2ZHk1NDIzM2FhMXc2",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)

    private val secondApiUrl = Base64.decode(
        "aHR0cHM6Ly9tYnBhcGkuc2hlZ3UubmV0L2FwaS9hcGlfY2xpZW50L2luZGV4Lw==",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)

    private val appKey = Base64.decode(
        "bW92aWVib3g=",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    private val appId = Base64.decode(
        "Y29tLnRkby5zaG93Ym94",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    private val appIdSecond = Base64.decode(
        "Y29tLm1vdmllYm94cHJvLmFuZHJvaWQ=",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    private const val APP_VERSION = "14.7"
    private const val APP_VERSION_CODE = "160"


    override suspend fun getHome(): List<Category> {
        val response = service.getHome(
            queryApi(
                mapOf(
                    "childmode" to "1",
                    "app_version" to APP_VERSION,
                    "appid" to appIdSecond,
                    "module" to "Home_list_type_v2",
                    "channel" to "Website",
                    "page" to "0",
                    "lang" to "en",
                    "type" to "all",
                    "pagelimit" to "10",
                    "expired_date" to "${getExpiryDate()}",
                    "platform" to "android"
                )
            )
        )

        return response.data.map { home ->
            Category(
                name = home.name?.takeIf { it.isNotEmpty() } ?: Category.FEATURED,
                list = home.list.mapNotNull { data ->
                    when (data.boxType) {
                        1 -> Movie(
                            id = data.id.toString(),
                            title = data.title ?: "",
                            quality = data.qualityTag,
                            rating = data.imdbRating?.toDoubleOrNull(),
                            poster = data.poster,
                            banner = data.bannerMini,
                        )

                        2 -> TvShow(
                            id = data.id.toString(),
                            title = data.title ?: "",
                            rating = data.imdbRating?.toDoubleOrNull(),
                            poster = data.poster,
                            banner = data.bannerMini,

                            seasons = data.seasonEpisode
                                ?.takeIf { it.matches("S\\d+\\s* E\\d+".toRegex()) }
                                ?.let {
                                    val result = Regex("S(\\d+)\\s* E(\\d+)").find(it)?.groupValues
                                    object {
                                        val season = result?.getOrNull(1)?.toIntOrNull() ?: 0

                                        val episode = result?.getOrNull(2)?.toIntOrNull() ?: 0
                                    }
                                }
                                ?.let { lastEpisode ->
                                    listOf(
                                        Season(
                                            id = "",
                                            number = lastEpisode.season,

                                            episodes = listOf(
                                                Episode(
                                                    id = "",
                                                    number = lastEpisode.episode,
                                                )
                                            )
                                        )
                                    )
                                }
                                ?: listOf()
                        )

                        else -> null
                    }
                }
            )
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            return listOf()
        }

        val response = service.search(
            url,
            queryApi(
                mapOf(
                    "childmode" to "1",
                    "app_version" to "11.5",
                    "appid" to appId,
                    "module" to "Search3",
                    "channel" to "Website",
                    "page" to page.toString(),
                    "lang" to "en",
                    "type" to "all",
                    "keyword" to query,
                    "pagelimit" to "20",
                    "expired_date" to "${getExpiryDate()}",
                    "platform" to "android",
                )
            )
        )

        return response.data.mapNotNull { data ->
            when (data.boxType) {
                1 -> Movie(
                    id = data.id.toString(),
                    title = data.title ?: "",
                    overview = data.description,
                    released = data.year?.toString(),
                    runtime = data.runtime,
                    quality = data.qualityTag,
                    rating = data.imdbRating?.toDoubleOrNull(),
                    poster = data.poster,
                    banner = data.bannerMini,
                )

                2 -> TvShow(
                    id = data.id.toString(),
                    title = data.title ?: "",
                    overview = data.description,
                    released = data.year?.toString(),
                    runtime = data.runtime,
                    quality = data.qualityTag,
                    rating = data.imdbRating?.toDoubleOrNull(),
                    poster = data.poster,
                    banner = data.bannerMini,
                )

                else -> null
            }
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        // TODO
        throw Exception("Not yet implemented")
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        // TODO
        throw Exception("Not yet implemented")
    }

    override suspend fun getMovie(id: String): Movie {
        val data = service.getMovieById(
            queryApi(
                mapOf(
                    "childmode" to "1",
                    "uid" to "",
                    "app_version" to "11.5",
                    "appid" to appId,
                    "module" to "Movie_detail",
                    "channel" to "Website",
                    "mid" to id,
                    "lang" to "en",
                    "expired_date" to "${getExpiryDate()}",
                    "platform" to "android",
                    "oss" to "",
                    "group" to "",
                )
            )
        ).data

        val movie = Movie(
            id = data.id.toString(),
            title = data.title ?: "",
            overview = data.description,
            released = data.released,
            runtime = data.runtime,
            trailer = data.trailer,
            quality = data.qualityTag,
            rating = data.imdbRating?.toDoubleOrNull(),
            poster = data.poster,

            genres = data.cats?.split(",")?.map {
                Genre(
                    id = it,
                    name = it.trim(),
                )
            } ?: listOf(),
            directors = data.director?.split(",")?.map {
                People(
                    id = it,
                    name = it.trim(),
                )
            } ?: listOf(),
            cast = data.actors?.split(",")?.map {
                People(
                    id = it,
                    name = it.trim(),
                )
            } ?: listOf(),
            recommendations = data.recommend.map {
                Movie(
                    id = it.mid?.toString() ?: "",
                    title = it.title ?: "",
                    released = it.year?.toString(),
                    runtime = it.runtime,
                    quality = it.qualityTag,
                    rating = it.imdbRating?.toDoubleOrNull(),
                    poster = it.poster,

                    genres = it.cats?.split(",")?.map { genre ->
                        Genre(
                            id = genre,
                            name = genre.trim()
                        )
                    } ?: listOf(),
                )
            },
        )

        return movie
    }

    override suspend fun getTvShow(id: String): TvShow {
        val data = service.getTvShowById(
            queryApi(
                mapOf(
                    "childmode" to "1",
                    "uid" to "",
                    "app_version" to "11.5",
                    "appid" to appId,
                    "module" to "TV_detail_1",
                    "display_all" to "1",
                    "channel" to "Website",
                    "lang" to "en",
                    "expired_date" to "${getExpiryDate()}",
                    "platform" to "android",
                    "tid" to id,
                )
            )
        ).data

        return TvShow(
            id = data.id.toString(),
            title = data.title ?: "",
            overview = data.description,
            released = data.released,
            trailer = data.trailerUrl,
            rating = data.imdbRating?.toDoubleOrNull(),
            poster = data.poster,

            seasons = data.season.map { number ->
                Season(
                    id = "$id-$number",
                    number = number,
                )
            },
            genres = data.cats?.split(",")?.map {
                Genre(
                    id = it,
                    name = it.trim(),
                )
            } ?: listOf(),
            directors = data.director?.split(",")?.map {
                People(
                    id = it,
                    name = it.trim(),
                )
            } ?: listOf(),
            cast = data.actors?.split(",")?.map {
                People(
                    id = it,
                    name = it.trim(),
                )
            } ?: listOf(),
        )
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (tvShowId, seasonNumber) = seasonId.split("-")

        val response = service.getEpisodes(
            queryApi(
                mapOf(
                    "childmode" to "1",
                    "app_version" to APP_VERSION,
                    "year" to "0",
                    "appid" to appIdSecond,
                    "module" to "TV_episode",
                    "display_all" to "1",
                    "channel" to "Website",
                    "season" to seasonNumber,
                    "lang" to "en",
                    "expired_date" to "${getExpiryDate()}",
                    "platform" to "android",
                    "tid" to tvShowId
                )
            )
        )

        val episodes = response.data.map {
            Episode(
                id = it.id.toString(),
                number = it.episode,
                title = it.title,
                released = it.released,
                poster = it.thumbs
            )
        }

        return episodes
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        // TODO
        throw Exception("Not yet implemented")
    }

    override suspend fun getPeople(id: String, page: Int): People {
        // TODO
        throw Exception("Not yet implemented")
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val sources = when (videoType) {
            is Video.Type.Movie -> {
                service.getMovieSourceById(
                    queryApi(
                        mapOf(
                            "childmode" to "1",
                            "uid" to "",
                            "app_version" to "11.5",
                            "appid" to appId,
                            "module" to "Movie_downloadurl_v3",
                            "channel" to "Website",
                            "mid" to id,
                            "lang" to "",
                            "expired_date" to "${getExpiryDate()}",
                            "platform" to "android",
                            "oss" to "1",
                            "group" to "",
                        )
                    )
                ).data
            }

            is Video.Type.Episode -> {
                service.getEpisodeSources(
                    queryApi(
                        mapOf(
                            "childmode" to "1",
                            "app_version" to "11.5",
                            "module" to "TV_downloadurl_v3",
                            "channel" to "Website",
                            "episode" to "${videoType.number}",
                            "expired_date" to "${getExpiryDate()}",
                            "platform" to "android",
                            "tid" to id,
                            "oss" to "1",
                            "uid" to "",
                            "appid" to appId,
                            "season" to "${videoType.season.number}",
                            "lang" to "en",
                            "group" to "",
                        )
                    )
                ).data
            }
        }

        val fid = sources.list.firstOrNull { it.fid != null }?.fid ?: 0

        val subtitles = when (videoType) {
            is Video.Type.Movie -> {
                service.getMovieSubtitlesById(
                    queryApi(
                        mapOf(
                            "childmode" to "1",
                            "fid" to fid.toString(),
                            "uid" to "",
                            "app_version" to "11.5",
                            "appid" to appId,
                            "module" to "Movie_srt_list_v2",
                            "channel" to "Website",
                            "mid" to id,
                            "lang" to "en",
                            "expired_date" to "${getExpiryDate()}",
                            "platform" to "android",
                        )
                    )
                ).data
            }

            is Video.Type.Episode -> {
                service.getEpisodeSubtitles(
                    queryApi(
                        mapOf(
                            "childmode" to "1",
                            "fid" to "$fid",
                            "app_version" to "11.5",
                            "module" to "TV_srt_list_v2",
                            "channel" to "Website",
                            "episode" to "${videoType.number}",
                            "expired_date" to "${getExpiryDate()}",
                            "platform" to "android",
                            "tid" to id,
                            "uid" to "",
                            "appid" to appId,
                            "season" to "${videoType.season.number}",
                            "lang" to "en",
                        )
                    )
                ).data
            }
        }

        return sources.list
            .filter { it.path?.isNotEmpty() ?: false }
            .mapIndexed { index, link ->
                Video.Server(
                    id = index.toString(),
                    name = "${link.quality} • ${link.size}",
                ).apply {
                    video = Video(
                        source = link.path ?: "",
                        subtitles = subtitles.list.flatMap {
                            it.subtitles
                                .filter { subtitle -> subtitle.filePath?.isNotEmpty() ?: false }
                                .map { subtitle ->
                                    Video.Subtitle(
                                        label = subtitle.language ?: "",
                                        file = subtitle.filePath ?: "",
                                    )
                                }
                        }
                    )
                }
            }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return server.video ?: throw Exception("No source found")
    }


    // Random 32 length string
    private fun randomToken(): String {
        return (0..31).joinToString("") {
            (('0'..'9') + ('a'..'f')).random().toString()
        }
    }

    private val token = randomToken()

    private object CipherUtils {
        private const val ALGORITHM = "DESede"
        private const val TRANSFORMATION = "DESede/CBC/PKCS5Padding"
        fun encrypt(str: String, key: String, iv: String): String? {
            return try {
                val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
                val bArr = ByteArray(24)
                val bytes: ByteArray = key.toByteArray()
                var length = if (bytes.size <= 24) bytes.size else 24
                System.arraycopy(bytes, 0, bArr, 0, length)
                while (length < 24) {
                    bArr[length] = 0
                    length++
                }
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(bArr, ALGORITHM),
                    IvParameterSpec(iv.toByteArray())
                )

                Base64.encode(cipher.doFinal(str.toByteArray()), 2).toString(Charsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun md5(str: String): String? {
            return MD5Util.md5(str)?.let { HexDump.toHexString(it).lowercase() }
        }

        fun getVerify(str: String?, str2: String, str3: String): String? {
            if (str != null) {
                return md5(md5(str2) + str3 + str)
            }
            return null
        }
    }

    private object HexDump {
        private val HEX_DIGITS = charArrayOf(
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'A',
            'B',
            'C',
            'D',
            'E',
            'F'
        )

        @JvmOverloads
        fun toHexString(bArr: ByteArray, i: Int = 0, i2: Int = bArr.size): String {
            val cArr = CharArray(i2 * 2)
            var i3 = 0
            for (i4 in i until i + i2) {
                val b = bArr[i4].toInt()
                val i5 = i3 + 1
                val cArr2 = HEX_DIGITS
                cArr[i3] = cArr2[b ushr 4 and 15]
                i3 = i5 + 1
                cArr[i5] = cArr2[b and 15]
            }
            return String(cArr)
        }
    }

    private object MD5Util {
        fun md5(str: String): ByteArray? {
            return this.md5(str.toByteArray())
        }

        fun md5(bArr: ByteArray?): ByteArray? {
            return try {
                val digest = MessageDigest.getInstance("MD5")
                digest.update(bArr ?: return null)
                digest.digest()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun queryApi(query: Map<String, String>): Map<String, String> {
        val encryptedQuery = CipherUtils.encrypt(JSONObject(query).toString(), key, iv)!!
        val appKeyHash = CipherUtils.md5(appKey)!!

        val newBody = JSONObject(
            mapOf(
                "app_key" to appKeyHash,
                "verify" to CipherUtils.getVerify(
                    encryptedQuery,
                    appKey,
                    key
                ),
                "encrypt_data" to encryptedQuery,
            )
        ).toString()
        val base64Body = Base64.encode(
            newBody.toByteArray(),
            Base64.NO_WRAP
        ).toString(Charsets.UTF_8)

        return mapOf(
            "data" to base64Body,
            "appid" to "27",
            "platform" to "android",
            "version" to APP_VERSION_CODE,
            // Probably best to randomize this
            "medium" to "Website&token$token"
        )
    }

    private fun getExpiryDate(): Long {
        // Current time + 12 hours
        return System.currentTimeMillis() + 60 * 60 * 12
    }


    private interface SuperStreamApiService {

        companion object {
            fun build(): SuperStreamApiService {
                val client = OkHttpClient.Builder().addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                        .addHeader("Accept", "charset=utf-8")
                        .addHeader("Platform", "android")

                    chain.proceed(requestBuilder.build())
                }.build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(SuperStreamApiService::class.java)
            }
        }


        @POST(".")
        @FormUrlEncoded
        suspend fun getHome(
            @FieldMap data: Map<String, String>,
        ): Response<List<HomeResponse>>

        @POST
        @FormUrlEncoded
        suspend fun search(
            @Url url: String,
            @FieldMap data: Map<String, String>,
        ): Response<List<Show>>


        @POST(".")
        @FormUrlEncoded
        suspend fun getMovieById(
            @FieldMap data: Map<String, String>,
        ): Response<MovieDetails>

        @POST(".")
        @FormUrlEncoded
        suspend fun getMovieSourceById(
            @FieldMap data: Map<String, String>,
        ): Response<LinkResponse>

        @POST(".")
        @FormUrlEncoded
        suspend fun getMovieSubtitlesById(
            @FieldMap data: Map<String, String>,
        ): Response<PrivateSubtitleData>


        @POST(".")
        @FormUrlEncoded
        suspend fun getTvShowById(
            @FieldMap data: Map<String, String>,
        ): Response<TvShow>

        @POST(".")
        @FormUrlEncoded
        suspend fun getEpisodes(
            @FieldMap data: Map<String, String>,
        ): Response<List<Episode>>

        @POST(".")
        @FormUrlEncoded
        suspend fun getEpisodeSources(
            @FieldMap data: Map<String, String>,
        ): Response<LinkResponse>

        @POST(".")
        @FormUrlEncoded
        suspend fun getEpisodeSubtitles(
            @FieldMap data: Map<String, String>,
        ): Response<PrivateSubtitleData>


        data class Response<T>(
            @SerializedName("code") val code: Int,
            @SerializedName("msg") val msg: String,
            @SerializedName("data") val data: T,
        )

        data class HomeResponse(
            @SerializedName("code") val code: Int? = null,
            @SerializedName("type") val type: String? = null,
            @SerializedName("name") val name: String? = null,
            @SerializedName("box_type") val boxType: Int? = null,
            @SerializedName("list") val list: List<Show> = listOf(),
            @SerializedName("cache") val cache: Boolean? = null,
            @SerializedName("cacheKey") val cacheKey: String? = null,
        )

        data class Show(
            @SerializedName("id") val id: Int,
            @SerializedName("box_type") val boxType: Int,
            @SerializedName("title") val title: String? = null,
            @SerializedName("actors") val actors: String? = null,
            @SerializedName("description") val description: String? = null,
            @SerializedName("poster_min") val posterMin: String? = null,
            @SerializedName("poster_org") val posterOrg: String? = null,
            @SerializedName("poster") val poster: String? = null,
            @SerializedName("banner_mini") val bannerMini: String? = null,
            @SerializedName("cats") val cats: String? = null,
//            @SerializedName("content_rating") val contentRating: Int? = null,
            @SerializedName("runtime") val runtime: Int? = null,
            @SerializedName("year") val year: Int? = null,
//            @SerializedName("collect") val collect: Int? = null,
            @SerializedName("view") val view: Int? = null,
            @SerializedName("download") val download: Int? = null,
            @SerializedName("imdb_rating") val imdbRating: String? = null,
//            @SerializedName("is_collect") val isCollect: Int? = null,
            @SerializedName("3d") val threeD: Int? = null,
            @SerializedName("audio_lang") val audioLang: String? = null,
            @SerializedName("quality_tag") val qualityTag: String? = null,
            @SerializedName("lang") val lang: String? = null,
            @SerializedName("trailer_url_arr") val trailerUrlArr: List<Trailer> = listOf(),
            @SerializedName("trailer_url") val trailerUrl: String? = null,

            @SerializedName("poster_2") val poster2: String? = null,
            @SerializedName("season_episode") val seasonEpisode: String? = null,
        )

        data class Trailer(
            @SerializedName("url") val url: String? = null,
            @SerializedName("img") val img: String? = null,
        )

        data class MovieDetails(
            @SerializedName("id") val id: Int,
            @SerializedName("mb_id") val mbId: Int? = null,
            @SerializedName("title") val title: String? = null,
            @SerializedName("vip_only") val vipOnly: Int? = null,
            @SerializedName("display") val display: Int? = null,
            @SerializedName("state") val state: Int? = null,
            @SerializedName("source_file") val sourceFile: Int? = null,
            @SerializedName("code_file") val codeFile: Int? = null,
            @SerializedName("director") val director: String? = null,
            @SerializedName("writer") val writer: String? = null,
            @SerializedName("actors") val actors: String? = null,
            @SerializedName("runtime") val runtime: Int? = null,
            @SerializedName("poster") val poster: String? = null,
            @SerializedName("poster_imdb") val posterImdb: Int? = null,
            @SerializedName("description") val description: String? = null,
            @SerializedName("cats") val cats: String? = null,
            @SerializedName("year") val year: Int? = null,
            @SerializedName("imdb_id") val imdbId: String? = null,
            @SerializedName("imdb_rating") val imdbRating: String? = null,
            @SerializedName("trailer") val trailer: String? = null,
            @SerializedName("recommend") val recommend: ArrayList<Recommend> = arrayListOf(),
            @SerializedName("add_time") val addTime: Int? = null,
//            @SerializedName("collect") val collect: Int? = null,
            @SerializedName("view") val view: Int? = null,
            @SerializedName("download") val download: Int? = null,
            @SerializedName("released") val released: String? = null,
            @SerializedName("released_timestamp") val releasedTimestamp: Int? = null,
            @SerializedName("update_time") val updateTime: Int? = null,
            @SerializedName("audio_lang") val audioLang: ArrayList<String> = arrayListOf(),
            @SerializedName("quality_tag") val qualityTag: String? = null,
            @SerializedName("quality_tag_new") val qualityTagNew: String? = null,
            @SerializedName("3d") val threeD: Int? = null,
            @SerializedName("remark") val remark: String? = null,
            @SerializedName("pending") val pending: String? = null,
            @SerializedName("content_rating") val contentRating: String? = null,
            @SerializedName("tmdb_id") val tmdbId: Int? = null,
            @SerializedName("tomato_url") val tomatoUrl: String? = null,
            @SerializedName("tomato_meter") val tomatoMeter: Int? = null,
            @SerializedName("tomato_meter_count") val tomatoMeterCount: Int? = null,
            @SerializedName("tomato_meter_state") val tomatoMeterState: String? = null,
            @SerializedName("reelgood_url") val reelgoodUrl: String? = null,
            @SerializedName("audience_score") val audienceScore: Int? = null,
            @SerializedName("audience_score_count") val audienceScoreCount: Int? = null,
            @SerializedName("no_tomato_url") val noTomatoUrl: Int? = null,
            @SerializedName("weights") val weights: Double? = null,
            @SerializedName("1080p") val quality1080p: Int? = null,
            @SerializedName("short_tag") val shortTag: String? = null,
            @SerializedName("poster_min") val posterMin: String? = null,
            @SerializedName("poster_org") val posterOrg: String? = null,
            @SerializedName("trailer_url_arr") val trailerUrlArr: ArrayList<Trailer> = arrayListOf(),
            @SerializedName("trailer_url") val trailerUrl: String? = null,
            @SerializedName("quality") val quality: ArrayList<String> = arrayListOf(),
            @SerializedName("quality_tags") val qualityTags: ArrayList<String> = arrayListOf(),
            @SerializedName("imdb_link") val imdbLink: String? = null,
            @SerializedName("seconds") val seconds: Int? = null,
//            @SerializedName("is_collect") val isCollect: Int? = null,
            @SerializedName("box_type") val boxType: Int? = null,
            @SerializedName("language") val language: ArrayList<Language> = arrayListOf(),
            @SerializedName("playlists") val playlists: ArrayList<Playlists> = arrayListOf()
        )

        data class Recommend(
            @SerializedName("mid") val mid: Int? = null,
            @SerializedName("title") val title: String? = null,
            @SerializedName("poster_min") val posterMin: String? = null,
            @SerializedName("poster_org") val posterOrg: String? = null,
            @SerializedName("poster") val poster: String? = null,
            @SerializedName("quality_tag") val qualityTag: String? = null,
            @SerializedName("imdb_rating") val imdbRating: String? = null,
            @SerializedName("audio_lang") val audioLang: String? = null,
            @SerializedName("runtime") val runtime: Int? = null,
            @SerializedName("cats") val cats: String? = null,
            @SerializedName("year") val year: Int? = null,
            @SerializedName("3d") val threeD: Int? = null

        )

        data class Playlists(
            @SerializedName("lid") val lid: Int? = null,
            @SerializedName("name") val name: String? = null,
            @SerializedName("username") val username: String? = null,
            @SerializedName("count") val count: Int? = null,
            @SerializedName("imgArr") val imgArr: ArrayList<String> = arrayListOf(),
            @SerializedName("imgArr_min") val imgArrMin: ArrayList<String> = arrayListOf(),
            @SerializedName("imgArr_org") val imgArrOrg: ArrayList<String> = arrayListOf()
        )

        data class TvShow(
            @SerializedName("id") val id: Int,
            @SerializedName("mb_id") val mbId: Int? = null,
            @SerializedName("title") val title: String? = null,
            @SerializedName("display") val display: Int? = null,
            @SerializedName("state") val state: Int? = null,
            @SerializedName("vip_only") val vipOnly: Int? = null,
            @SerializedName("code_file") val codeFile: Int? = null,
            @SerializedName("director") val director: String? = null,
            @SerializedName("writer") val writer: String? = null,
            @SerializedName("actors") val actors: String? = null,
            @SerializedName("add_time") val addTime: Int? = null,
            @SerializedName("poster") val poster: String? = null,
            @SerializedName("poster_imdb") val posterImdb: Int? = null,
            @SerializedName("banner_mini") val bannerMini: String? = null,
            @SerializedName("description") val description: String? = null,
            @SerializedName("imdb_id") val imdbId: String? = null,
            @SerializedName("cats") val cats: String? = null,
            @SerializedName("year") val year: Int? = null,
//            @SerializedName("collect") val collect: Int? = null,
            @SerializedName("view") val view: Int? = null,
            @SerializedName("download") val download: Int? = null,
            @SerializedName("update_time") val updateTime: String? = null,
            @SerializedName("released") val released: String? = null,
            @SerializedName("released_timestamp") val releasedTimestamp: Int? = null,
            @SerializedName("episode_released") val episodeReleased: String? = null,
            @SerializedName("episode_released_timestamp") val episodeReleasedTimestamp: Int? = null,
            @SerializedName("max_season") val maxSeason: Int? = null,
            @SerializedName("max_episode") val maxEpisode: Int? = null,
            @SerializedName("remark") val remark: String? = null,
            @SerializedName("imdb_rating") val imdbRating: String? = null,
            @SerializedName("content_rating") val contentRating: String? = null,
            @SerializedName("tmdb_id") val tmdbId: Int? = null,
            @SerializedName("tomato_url") val tomatoUrl: String? = null,
            @SerializedName("tomato_meter") val tomatoMeter: Int? = null,
            @SerializedName("tomato_meter_count") val tomatoMeterCount: Int? = null,
            @SerializedName("tomato_meter_state") val tomatoMeterState: String? = null,
            @SerializedName("reelgood_url") val reelgoodUrl: String? = null,
            @SerializedName("audience_score") val audienceScore: Int? = null,
            @SerializedName("audience_score_count") val audienceScoreCount: Int? = null,
            @SerializedName("no_tomato_url") val noTomatoUrl: Int? = null,
            @SerializedName("order_year") val orderYear: Int? = null,
            @SerializedName("episodate_id") val episodateId: String? = null,
            @SerializedName("weights_day") val weightsDay: Double? = null,
            @SerializedName("short_tag") val shortTag: String? = null,
            @SerializedName("poster_min") val posterMin: String? = null,
            @SerializedName("poster_org") val posterOrg: String? = null,
            @SerializedName("banner_mini_min") val bannerMiniMin: String? = null,
            @SerializedName("banner_mini_org") val bannerMiniOrg: String? = null,
            @SerializedName("trailer_url") val trailerUrl: String? = null,
            @SerializedName("years") val years: ArrayList<Int> = arrayListOf(),
            @SerializedName("season") val season: ArrayList<Int> = arrayListOf(),
            @SerializedName("history") val history: ArrayList<String> = arrayListOf(),
            @SerializedName("imdb_link") val imdbLink: String? = null,
            @SerializedName("episode") val episode: ArrayList<Episode> = arrayListOf(),
//            @SerializedName("is_collect") val isCollect: Int? = null,
            @SerializedName("language") val language: ArrayList<Language> = arrayListOf(),
            @SerializedName("box_type") val boxType: Int? = null,
            @SerializedName("year_year") val yearYear: String? = null,
            @SerializedName("season_episode") val seasonEpisode: String? = null
        )

        data class Language(
            @SerializedName("title") val title: String? = null,
            @SerializedName("lang") val lang: String? = null
        )

        data class Episode(
            @SerializedName("id") val id: Int,
            @SerializedName("tid") val tid: Int? = null,
            @SerializedName("mb_id") val mbId: Int? = null,
            @SerializedName("imdb_id") val imdbId: String? = null,
            @SerializedName("imdb_id_status") val imdbIdStatus: Int? = null,
            @SerializedName("srt_status") val srtStatus: Int? = null,
            @SerializedName("season") val season: Int,
            @SerializedName("episode") val episode: Int,
            @SerializedName("state") val state: Int? = null,
            @SerializedName("title") val title: String? = null,
            @SerializedName("thumbs") val thumbs: String? = null,
            @SerializedName("thumbs_bak") val thumbsBak: String? = null,
            @SerializedName("thumbs_original") val thumbsOriginal: String? = null,
            @SerializedName("poster_imdb") val posterImdb: Int? = null,
            @SerializedName("synopsis") val synopsis: String? = null,
            @SerializedName("runtime") val runtime: Int? = null,
            @SerializedName("view") val view: Int? = null,
            @SerializedName("download") val download: Int? = null,
            @SerializedName("source_file") val sourceFile: Int? = null,
            @SerializedName("code_file") val codeFile: Int? = null,
            @SerializedName("add_time") val addTime: Int? = null,
            @SerializedName("update_time") val updateTime: Int? = null,
            @SerializedName("released") val released: String? = null,
            @SerializedName("released_timestamp") val releasedTimestamp: Long? = null,
            @SerializedName("audio_lang") val audioLang: String? = null,
            @SerializedName("quality_tag") val qualityTag: String? = null,
            @SerializedName("3d") val threeD: Int? = null,
            @SerializedName("remark") val remark: String? = null,
            @SerializedName("pending") val pending: String? = null,
            @SerializedName("imdb_rating") val imdbRating: String? = null,
            @SerializedName("display") val display: Int? = null,
            @SerializedName("sync") val sync: Int? = null,
            @SerializedName("tomato_meter") val tomatoMeter: Int? = null,
            @SerializedName("tomato_meter_count") val tomatoMeterCount: Int? = null,
            @SerializedName("tomato_audience") val tomatoAudience: Int? = null,
            @SerializedName("tomato_audience_count") val tomatoAudienceCount: Int? = null,
            @SerializedName("thumbs_min") val thumbsMin: String? = null,
            @SerializedName("thumbs_org") val thumbsOrg: String? = null,
            @SerializedName("imdb_link") val imdbLink: String? = null,
//        @SerializedName("quality_tags") val qualityTags: List<String> = listOf(),
//        @SerializedName("play_progress") val playProgress: PlayProgress? = PlayProgress()
        )


        data class LinkResponse(
            @SerializedName("seconds") val seconds: Int? = null,
            @SerializedName("quality") val quality: List<String> = listOf(),
            @SerializedName("list") val list: List<Link> = listOf(),
        ) {
            data class Link(
                @SerializedName("path") val path: String? = null,
                @SerializedName("quality") val quality: String? = null,
                @SerializedName("real_quality") val realQuality: String? = null,
                @SerializedName("format") val format: String? = null,
                @SerializedName("size") val size: String? = null,
                @SerializedName("size_bytes") val sizeBytes: Long? = null,
                @SerializedName("count") val count: Int? = null,
                @SerializedName("dateline") val dateline: Long? = null,
                @SerializedName("fid") val fid: Int? = null,
                @SerializedName("mmfid") val mmfid: Int? = null,
                @SerializedName("h265") val h265: Int? = null,
                @SerializedName("hdr") val hdr: Int? = null,
                @SerializedName("filename") val filename: String? = null,
                @SerializedName("original") val original: Int? = null,
                @SerializedName("colorbit") val colorbit: Int? = null,
                @SerializedName("success") val success: Int? = null,
                @SerializedName("timeout") val timeout: Int? = null,
                @SerializedName("vip_link") val vipLink: Int? = null,
                @SerializedName("fps") val fps: Int? = null,
                @SerializedName("bitstream") val bitstream: String? = null,
                @SerializedName("width") val width: Int? = null,
                @SerializedName("height") val height: Int? = null,
            )
        }

        data class PrivateSubtitleData(
            @SerializedName("select") val select: ArrayList<String> = arrayListOf(),
            @SerializedName("list") val list: ArrayList<SubtitleList> = arrayListOf()
        ) {
            data class SubtitleList(
                @SerializedName("language") val language: String? = null,
                @SerializedName("subtitles") val subtitles: ArrayList<Subtitles> = arrayListOf()
            ) {
                data class Subtitles(
                    @SerializedName("sid") val sid: Int? = null,
                    @SerializedName("mid") val mid: String? = null,
                    @SerializedName("file_path") val filePath: String? = null,
                    @SerializedName("lang") val lang: String? = null,
                    @SerializedName("language") val language: String? = null,
                    @SerializedName("delay") val delay: Int? = null,
                    @SerializedName("point") val point: String? = null,
                    @SerializedName("order") val order: Int? = null,
                    @SerializedName("admin_order") val adminOrder: Int? = null,
                    @SerializedName("myselect") val myselect: Int? = null,
                    @SerializedName("add_time") val addTime: Long? = null,
                    @SerializedName("count") val count: Int? = null
                )
            }
        }
    }
}