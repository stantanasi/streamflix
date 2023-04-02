package com.tanasi.sflix.utils

import com.tanasi.sflix.BuildConfig
import kotlin.math.max

object InAppUpdater {

    private const val GITHUB_OWNER = "stantanasi"
    private const val GITHUB_REPO = "sflix"

    private data class Version(val name: String) : Comparable<Version> {
        override operator fun compareTo(other: Version): Int {
            val thisParts = this.name.split(".").toTypedArray()
            val thatParts = other.name.split(".").toTypedArray()
            for (i in 0 until max(thisParts.size, thatParts.size)) {
                val thisPart = thisParts.getOrNull(i)?.toIntOrNull() ?: 0
                val thatPart = thatParts.getOrNull(i)?.toIntOrNull() ?: 0
                if (thisPart < thatPart) return -1
                if (thisPart > thatPart) return 1
            }
            return 0
        }
    }

    suspend fun getReleaseUpdate(): GitHub.Release? {
        val latestRelease = GitHub.service.getLatestRelease(GITHUB_OWNER, GITHUB_REPO)

        val currentVersion = BuildConfig.VERSION_NAME

        if (Version(latestRelease.tagName.substringAfter("v")) > Version(currentVersion)) {
            return latestRelease
        }

        return null
    }
}