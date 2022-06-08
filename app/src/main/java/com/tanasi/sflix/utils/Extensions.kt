package com.tanasi.sflix.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

fun String.toCalendar(): Calendar? {
    val patterns = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("yyyy", Locale.ENGLISH),
    )
    patterns.forEach { sdf ->
        try {
            return Calendar.getInstance().also { it.time = sdf.parse(this)!! }
        } catch (e: Exception) {
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

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}