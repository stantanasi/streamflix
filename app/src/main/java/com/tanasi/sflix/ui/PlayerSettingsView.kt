package com.tanasi.sflix.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.exoplayer2.ExoPlayer
import com.tanasi.sflix.databinding.ViewPlayerSettingsBinding

class PlayerSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val binding = ViewPlayerSettingsBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    var player: ExoPlayer? = null
}