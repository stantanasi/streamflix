package com.tanasi.streamflix.utils

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

object GitHub {

    private val service = ApiService.build()


    object Releases {

        suspend fun getReleases(
            owner: String,
            repo: String,
        ): List<Release> {
            return service.getReleases(
                owner = owner,
                repo = repo,
            )
        }

        suspend fun getLatestRelease(
            owner: String,
            repo: String,
        ): Release {
            return service.getLatestRelease(
                owner = owner,
                repo = repo,
            )
        }
    }


    private interface ApiService {

        companion object {
            fun build(): ApiService {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.github.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(ApiService::class.java)
            }
        }

        @GET("repos/{owner}/{repo}/releases")
        suspend fun getReleases(
            @Path("owner") owner: String,
            @Path("repo") repo: String,
        ): List<Release>

        @GET("repos/{owner}/{repo}/releases/latest")
        suspend fun getLatestRelease(
            @Path("owner") owner: String,
            @Path("repo") repo: String,
        ): Release
    }


    data class Release(
        @SerializedName("url") val url: String,
        @SerializedName("html_url") val htmlUrl: String,
        @SerializedName("assets_url") val assetsUrl: String,
        @SerializedName("upload_url") val uploadUrl: String,
        @SerializedName("tarball_url") val tarballUrl: String?,
        @SerializedName("zipball_url") val zipballUrl: String?,
        @SerializedName("id") val id: Int,
        @SerializedName("node_id") val nodeId: String,
        @SerializedName("tag_name") val tagName: String,
        @SerializedName("target_commitish") val targetCommitish: String,
        @SerializedName("name") val name: String?,
        @SerializedName("body") val body: String? = null,
        @SerializedName("draft") val draft: Boolean,
        @SerializedName("prerelease") val prerelease: Boolean,
        @SerializedName("created_at") val createdAt: String,
        @SerializedName("published_at") val publishedAt: String?,
        @SerializedName("author") val author: User,
        @SerializedName("assets") val assets: List<Asset>,
        @SerializedName("body_html") val bodyHtml: String = "",
        @SerializedName("body_text") val bodyText: String = "",
        @SerializedName("mentions_count") val mentionsCount: Int = 0,
        @SerializedName("discussion_url") val discussionUrl: String = "",
        @SerializedName("reactions") val reactions: Reactions? = null,
    ) {

        data class Asset(
            @SerializedName("url") val url: String,
            @SerializedName("browser_download_url") val browserDownloadUrl: String,
            @SerializedName("id") val id: Int,
            @SerializedName("node_id") val nodeId: String,
            @SerializedName("name") val name: String,
            @SerializedName("label") val label: String?,
            @SerializedName("state") val state: String,
            @SerializedName("content_type") val contentType: String,
            @SerializedName("size") val size: Int,
            @SerializedName("download_count") val downloadCount: Int,
            @SerializedName("created_at") val createdAt: String,
            @SerializedName("updated_at") val updatedAt: String,
            @SerializedName("uploader") val uploader: User,
        )
    }

    data class User(
        @SerializedName("name") val name: String? = "",
        @SerializedName("email") val email: String? = "",
        @SerializedName("login") val login: String,
        @SerializedName("id") val id: Int,
        @SerializedName("node_id") val nodeId: String,
        @SerializedName("avatar_url") val avatarUrl: String,
        @SerializedName("gravatar_id") val gravatarId: String?,
        @SerializedName("url") val url: String,
        @SerializedName("html_url") val htmlUrl: String,
        @SerializedName("followers_url") val followersUrl: String,
        @SerializedName("following_url") val followingUrl: String,
        @SerializedName("gists_url") val gistsUrl: String,
        @SerializedName("starred_url") val starredUrl: String,
        @SerializedName("subscriptions_url") val subscriptionsUrl: String,
        @SerializedName("organizations_url") val organizationsUrl: String,
        @SerializedName("repos_url") val reposUrl: String,
        @SerializedName("events_url") val eventsUrl: String,
        @SerializedName("received_events_url") val receivedEventsUrl: String,
        @SerializedName("type") val type: String,
        @SerializedName("site_admin") val siteAdmin: Boolean,
        @SerializedName("starred_at") val starredAt: String = "",
    )

    data class Reactions(
        @SerializedName("url") val url: String,
        @SerializedName("total_count") val totalCount: Int,
        @SerializedName("+1") val plus1: Int,
        @SerializedName("-1") val minus1: Int,
        @SerializedName("laugh") val laugh: Int,
        @SerializedName("confused") val confused: Int,
        @SerializedName("heart") val heart: Int,
        @SerializedName("hooray") val hooray: Int,
        @SerializedName("eyes") val eyes: Int,
        @SerializedName("rocket") val rocket: Int,
    )
}