package com.tanasi.streamflix.ui

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import com.tanasi.streamflix.BuildConfig
import com.tanasi.streamflix.databinding.DialogUpdateMobileBinding
import com.tanasi.streamflix.utils.GitHub

class UpdateMobileDialog(context: Context) : Dialog(context) {

    private val binding = DialogUpdateMobileBinding.inflate(LayoutInflater.from(context))

    var release: GitHub.Release? = null
        set(value) {
            binding.tvUpdateNewVersion.text = value?.tagName?.substringAfter("v") ?: "-"
            binding.tvUpdateReleaseNotes.text = value?.body?.replace(
                Regex("^- ([a-z0-9]+: )?(.*?)(#\\d+ )?\$", RegexOption.MULTILINE),
                "- $2"
            )
            field = value
        }

    var isLoading: Boolean
        get() = binding.pbUpdateIsLoading.isVisible
        set(value) {
            binding.pbUpdateIsLoading.visibility = when {
                value -> View.VISIBLE
                else -> View.GONE
            }
        }

    init {
        setContentView(binding.root)

        binding.tvUpdateCurrentVersion.text = BuildConfig.VERSION_NAME

        binding.btnUpdateCancel.setOnClickListener {
            hide()
        }


        window?.setLayout(
            context.resources.displayMetrics.widthPixels,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }


    fun setOnUpdateClickListener(listener: (view: View) -> Unit) {
        binding.btnUpdate.setOnClickListener(listener)
    }
}