package com.tanasi.streamflix.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tanasi.streamflix.database.SerienStreamDatabase
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.providers.SerienStreamProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.io.File
import androidx.core.content.edit

class SerienStreamUpdateTvShowWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val PREFS_NAME = "SerienStreamUpdateTvShowsPrefs"
        private const val KEY_PROCESSED_FILE = "processed_serienstream_tvshows_json"
        private const val AFTER_BOOTUP_SERIENSTREAM ="after_bootup_serienstream"
    }

    private val dao = SerienStreamDatabase.getInstance(context).tvShowDao()
    private val provider = SerienStreamProvider
    private val gson = Gson()

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hasProcessedFile = prefs.getBoolean(KEY_PROCESSED_FILE, false)
            val isAfterBootUp = prefs.getBoolean(AFTER_BOOTUP_SERIENSTREAM, false)
            prefs.edit() { putBoolean(KEY_PROCESSED_FILE, false) }
            val assetManager = applicationContext.assets
            val inputStream = assetManager.open("serienstream_tvshows.json")

            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val semaphore = Semaphore(30)
            if (isAfterBootUp) {
                if (!hasProcessedFile) {
                    try {
                        val listType = object : TypeToken<List<TvShow>>() {}.type
                        val tvShowsFromJson: List<TvShow> =
                            gson.fromJson(jsonString, listType) ?: emptyList()

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
                                        Log.d(
                                            "SerienStreamWorker",
                                            "Updated from JSON: ${existing.title}"
                                        )
                                    }
                                }
                            }
                        }

                        updateJobs.awaitAll()
                        SerienStreamProvider.invalidateCache()
                        prefs.edit() { putBoolean(KEY_PROCESSED_FILE, true) }

                    } catch (e: Exception) {
                        Log.e("SerienStreamWorker", "Failed to process JSON", e)
                    }
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
                                Log.d("SerienStreamWorker", "Updated from provider: ${show.title}")
                            } catch (e: HttpException) {
                                Log.w("SerienStreamWorker", "404 for ${show.id}")
                            } catch (e: Exception) {
                                Log.e("SerienStreamWorker", "Error updating ${show.id}", e)
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }
            prefs.edit() { putBoolean(AFTER_BOOTUP_SERIENSTREAM, true) }
            SerienStreamProvider.invalidateCache()
            Log.d("SerienStreamWorker", "All updates completed")
            Result.success()

        } catch (e: Exception) {
            Log.e("SerienStreamWorker", "Worker failed", e)
            Result.retry()
        }

    }

}
