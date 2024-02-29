package com.tanasi.streamflix.utils

import android.content.Context
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram

object WatchNextUtils {

    fun programs(context: Context): List<WatchNextProgram> {
        return context.contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI,
            WatchNextProgram.PROJECTION,
            null,
            null,
            null
        )?.use { cursor ->
            cursor.map { WatchNextProgram.fromCursor(it) }
                .filter { it.internalProviderId == UserPreferences.currentProvider!!.name }
        } ?: listOf()
    }

    fun insert(context: Context, program: WatchNextProgram) {
        context.contentResolver.insert(
            TvContractCompat.WatchNextPrograms.CONTENT_URI,
            program.toContentValues(),
        )
    }

    fun getProgram(context: Context, contentId: String): WatchNextProgram? {
        return context.contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI,
            WatchNextProgram.PROJECTION,
            null,
            null,
            null
        )?.use { cursor ->
            cursor.map { WatchNextProgram.fromCursor(it) }
                .find { it.contentId == contentId && it.internalProviderId == UserPreferences.currentProvider!!.name }
        }
    }

    fun updateProgram(context: Context, id: Long, program: WatchNextProgram) {
        context.contentResolver.update(
            TvContractCompat.buildWatchNextProgramUri(id),
            program.toContentValues(),
            null,
            null,
        )
    }

    fun deleteProgramById(context: Context, id: Long) {
        context.contentResolver.delete(
            TvContractCompat.buildWatchNextProgramUri(id),
            null,
            null,
        )
    }
}