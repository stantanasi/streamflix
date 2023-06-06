package com.tanasi.streamflix.utils

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Tracks
import com.tanasi.streamflix.R
import com.tanasi.streamflix.activities.main.MainActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

fun String.toCalendar(): Calendar? {
    val patterns = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("yyyy", Locale.ENGLISH),
    )
    patterns.forEach { sdf ->
        try {
            return Calendar.getInstance().also { it.time = sdf.parse(this)!! }
        } catch (_: Exception) {
        }
    }
    return null
}

fun Calendar.format(pattern: String): String? {
    return try {
        SimpleDateFormat(pattern, Locale.ENGLISH).format(this.time)
    } catch (e: Exception) {
        null
    }
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.toActivity(): FragmentActivity? = this as? FragmentActivity

fun FragmentActivity.getCurrentFragment(): Fragment? = when (this) {
    is MainActivity -> {
        val navHostFragment = this.supportFragmentManager
            .findFragmentById(R.id.nav_main_fragment) as NavHostFragment
        navHostFragment.childFragmentManager.fragments.firstOrNull()
    }
    else -> null
}

suspend fun <T> retry(retries: Int, predicate: suspend (attempt: Int) -> T): T {
    require(retries > 0) { "Expected positive amount of retries, but had $retries" }
    var throwable: Throwable? = null
    (1..retries).forEach { attempt ->
        try {
            return predicate(attempt)
        } catch (e: Throwable) {
            throwable = e
        }
    }
    throw throwable!!
}

fun <T> Cursor.map(transform: (Cursor) -> T): List<T> {
    val items = mutableListOf<T>()
    while (!this.isClosed && this.moveToNext()) {
        items.add(transform(this))
    }
    this.close()
    return items.toList()
}

val Tracks.Group.trackFormats: List<Format>
    get() {
        val trackFormats = mutableListOf<Format>()
        for (trackIndex in 0 until this.length) {
            if (!this.isTrackSupported(trackIndex)) {
                continue
            }
            trackFormats.add(this.getTrackFormat(trackIndex))
        }
        return trackFormats
    }

fun <T> List<T>.findClosest(value: Float, selector: (T) -> Float): T? {
    return minByOrNull { abs(value - selector(it)) }
}

inline fun <reified T : ViewModel> Fragment.viewModelsFactory(crossinline viewModelInitialization: () -> T): Lazy<T> {
    return viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return viewModelInitialization.invoke() as T
            }
        }
    }
}