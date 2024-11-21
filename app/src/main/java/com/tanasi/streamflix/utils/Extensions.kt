package com.tanasi.streamflix.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Format
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks
import androidx.navigation.fragment.NavHostFragment
import com.tanasi.streamflix.R
import com.tanasi.streamflix.activities.main.MainMobileActivity
import com.tanasi.streamflix.activities.main.MainTvActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.parcelize.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun String.toCalendar(): Calendar? {
    val patterns = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("d MMMM yyyy ('USA')", Locale.ENGLISH),
        SimpleDateFormat("d MMMM yyyy", Locale.FRENCH),
        SimpleDateFormat("yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMMM d, yyyy ('United' 'States')", Locale.ENGLISH),
        SimpleDateFormat("MMM. d, yyyy", Locale.ENGLISH),
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
    is MainMobileActivity -> {
        val navHostFragment = this.supportFragmentManager
            .findFragmentById(R.id.nav_main_fragment) as NavHostFragment
        navHostFragment.childFragmentManager.fragments.firstOrNull()
    }

    is MainTvActivity -> {
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
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return viewModelInitialization.invoke() as T
            }
        }
    }
}

fun Int.dp(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}

inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
    if (layoutParams is T) block(layoutParams as T)
}

fun View.margin(
    left: Int? = null,
    top: Int? = null,
    right: Int? = null,
    bottom: Int? = null
) {
    layoutParams<ViewGroup.MarginLayoutParams> {
        left?.let { leftMargin = it }
        top?.let { topMargin = it }
        right?.let { rightMargin = it }
        bottom?.let { bottomMargin = it }
    }
}


fun Int.getAlpha(): Int = Color.alpha(this)

fun Int.setAlpha(alpha: Int): Int = Color.argb(
    alpha,
    Color.red(this),
    Color.green(this),
    Color.blue(this),
)

fun Int.getRgb(): Int = Color.rgb(
    Color.red(this),
    Color.green(this),
    Color.blue(this)
)

fun Int.setRgb(rgb: Int): Int = Color.argb(
    Color.alpha(this),
    Color.red(rgb),
    Color.green(rgb),
    Color.blue(rgb),
)


@Parcelize
data class MediaServer(
    val id: String,
    val name: String,
) : Parcelable

private val MediaMetadata.Builder.extras: Bundle?
    get() = this.javaClass.getDeclaredField("extras").let {
        it.isAccessible = true
        it.get(this) as Bundle?
    }

val MediaMetadata.mediaServerId: String?
    get() = this.extras
        ?.getString("mediaServerId")

val MediaMetadata.mediaServers: List<MediaServer>
    get() = this.extras
        ?.getParcelableArray("mediaServers")
        ?.map { it as MediaServer }
        ?: listOf()

fun MediaMetadata.Builder.setMediaServerId(mediaServerId: String) = this
    .setExtras((this.extras ?: Bundle()).also { bundle ->
        bundle.putString("mediaServerId", mediaServerId)
    })

fun MediaMetadata.Builder.setMediaServers(mediaServers: List<MediaServer>) = this
    .setExtras((this.extras ?: Bundle()).also { bundle ->
        bundle.putParcelableArray("mediaServers", mediaServers.toTypedArray())
    })

@Suppress("UNCHECKED_CAST")
fun <K, V> Map<K, V?>.filterNotNullValues() = filterValues { it != null } as Map<K, V>

fun <T> List<T>.safeSubList(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex > toIndex) return emptyList()
    return subList(
        max(min(fromIndex.coerceAtLeast(0), size), 0),
        max(min(toIndex.coerceAtMost(size), size), 0)
    )
}

inline fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(
    flow,
    flow2,
    flow3,
    flow4,
    flow5,
    flow6,
) { args: Array<*> ->
    @Suppress("UNCHECKED_CAST")
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
    )
}

fun String.toSubtitleMimeType(): String {
    return when {
        endsWith("vtt", true) -> MimeTypes.TEXT_VTT
        endsWith("srt", true) -> MimeTypes.APPLICATION_SUBRIP
        endsWith("xml", true) || endsWith("ttml", true) -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

inline fun <reified T : Enum<T>> T.next(): T {
    val values = enumValues<T>()
    val nextOrdinal = (ordinal + 1) % values.size
    return values[nextOrdinal]
}

fun <T> CoroutineScope.asyncOrNull(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T?> {
    return async(context, start) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("Extensions", "asyncOrNull: ", e)
            null
        }
    }
}

fun Uri.getFileName(context: Context): String? {
    return when (this.scheme) {
        ContentResolver.SCHEME_CONTENT -> context.contentResolver.getContentFileName(this)
        else -> this.path?.let { File(it) }?.name
    }
}

private fun ContentResolver.getContentFileName(uri: Uri): String? = runCatching {
    this.query(uri, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        return@use cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it != -1 }
            ?.let { cursor.getString(it) }
    }
}.getOrNull()

operator fun <K, V> Map<out K, V>.plus(map: Map<out K, V>): Map<K, V> =
    LinkedHashMap(this).apply { putAll(map) }