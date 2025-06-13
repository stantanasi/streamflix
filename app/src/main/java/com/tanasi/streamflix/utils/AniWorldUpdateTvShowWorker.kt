package com.tanasi.streamflix.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tanasi.streamflix.models.TvShow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.io.File
import androidx.core.content.edit
import com.tanasi.streamflix.database.AniWorldDatabase
import com.tanasi.streamflix.providers.AniWorldProvider

class AniWorldUpdateTvShowWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val PREFS_NAME = "AniWorldUpdateTvShowsPrefs"
        private const val KEY_PROCESSED_FILE = "processed_aniworld_tvshows_json"
    }

    private val dao = AniWorldDatabase.getInstance(context).tvShowDao()
    private val provider = AniWorldProvider
    private val gson = Gson()

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hasProcessedFile = prefs.getBoolean(KEY_PROCESSED_FILE, false)
            val file = File(applicationContext.filesDir, "aniworld_tvshows.json")
            val semaphore = Semaphore(30)

            if (file.exists() && !hasProcessedFile) {
                try {
                    val json = file.readText()
                    val listType = object : TypeToken<List<TvShow>>() {}.type
                    val tvShowsFromJson: List<TvShow> = gson.fromJson(json, listType) ?: emptyList()

                    val updateJobs = tvShowsFromJson.map { show ->
                        async {
                            semaphore.withPermit {
                                val existing = dao.getById(show.id)
                                if (existing != null && existing.poster == null) {
                                    val updated = existing.copy(
                                        poster = show.poster,
                                        overview = show.overview
                                    )
                                    dao.update(updated)
                                    Log.d("AniWorldWorker", "Updated from JSON: ${show.title}")
                                }
                            }
                        }
                    }

                    updateJobs.awaitAll()

                    prefs.edit() { putBoolean(KEY_PROCESSED_FILE, true) }

                } catch (e: Exception) {
                    Log.e("AniWorldWorker", "Failed to process JSON", e)
                }
                file.delete()
            }

            val showsToUpdate = dao.getAllWithNullPoster()
            val jobs = showsToUpdate.map { show ->
                async {
                    semaphore.withPermit {
                        try {
                            val detailed = provider.getTvShow(show.id)
                            val updated = show.copy(
                                poster = detailed.poster,
                                overview = detailed.overview
                            )
                            dao.update(updated)
                            Log.d("AniWorldWorker", "Updated from provider: ${show.title}")
                        } catch (e: HttpException) {
                            Log.w("AniWorldWorker", "404 for ${show.id}")
                        } catch (e: Exception) {
                            Log.e("AniWorldWorker", "Error updating ${show.id}", e)
                        }
                    }
                }
            }

            jobs.awaitAll()
            AniWorldProvider.invalidateCache()
            Log.d("AniWorldWorker", "All updates completed")
            Result.success()

        } catch (e: Exception) {
            Log.e("AniWorldWorker", "Worker failed", e)
            Result.retry()
        }
    }

}
