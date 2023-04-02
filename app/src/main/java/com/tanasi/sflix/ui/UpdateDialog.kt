package com.tanasi.sflix.ui

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import com.tanasi.sflix.BuildConfig
import com.tanasi.sflix.databinding.DialogUpdateBinding
import com.tanasi.sflix.utils.GitHub

class UpdateDialog(context: Context) : Dialog(context) {

    private val binding = DialogUpdateBinding.inflate(LayoutInflater.from(context))

    var release: GitHub.Release? = null
        set(value) {
            binding.tvUpdateNewVersion.text = value?.tagName?.substringAfter("v") ?: "-"
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

        binding.btnUpdate.requestFocus()


        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.55).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }


    fun setOnUpdateClickListener(listener: (view: View) -> Unit) {
        binding.btnUpdate.setOnClickListener(listener)
    }
}