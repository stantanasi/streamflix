package com.tanasi.sflix.ui

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.tanasi.sflix.databinding.DialogUpdateBinding

class UpdateDialog(context: Context) : Dialog(context) {

    private val binding = DialogUpdateBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
    }
}