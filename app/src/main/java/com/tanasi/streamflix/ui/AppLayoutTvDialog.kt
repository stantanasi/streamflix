package com.tanasi.streamflix.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.tanasi.streamflix.R
import com.tanasi.streamflix.activities.main.MainMobileActivity
import com.tanasi.streamflix.activities.main.MainTvActivity
import com.tanasi.streamflix.databinding.DialogAppLayoutTvBinding
import com.tanasi.streamflix.databinding.ItemSettingTvBinding
import com.tanasi.streamflix.utils.UserPreferences

class AppLayoutTvDialog(
    context: Context
) : Dialog(context) {

    private val binding = DialogAppLayoutTvBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)

        val items = UserPreferences.AppLayout.entries
            .map {
                it to ItemSettingTvBinding.inflate(
                    LayoutInflater.from(context),
                    binding.llAppLayout,
                    true
                )
            }
            .onEach { (it, itemBinding) ->
                itemBinding.ivSettingIcon.visibility = View.GONE

                itemBinding.vSettingColor.visibility = View.GONE

                itemBinding.tvSettingMainText.text = when (it) {
                    UserPreferences.AppLayout.AUTO -> context.getString(R.string.app_layout_auto)
                    UserPreferences.AppLayout.MOBILE -> context.getString(R.string.app_layout_mobile)
                    UserPreferences.AppLayout.TV -> context.getString(R.string.app_layout_tv)
                }

                itemBinding.tvSettingSubText.visibility = View.GONE

                itemBinding.ivSettingEnter.visibility = View.GONE

                itemBinding.ivSettingIsSelected.visibility = when {
                    UserPreferences.appLayout == it -> View.VISIBLE
                    UserPreferences.appLayout == null && it == UserPreferences.AppLayout.AUTO -> View.VISIBLE
                    else -> View.GONE
                }
            }
        items.forEach { (_, item) ->
            item.root.setOnClickListener {
                items.forEach { (_, item) -> item.ivSettingIsSelected.visibility = View.GONE }
                item.ivSettingIsSelected.visibility = View.VISIBLE
            }
        }

        binding.btnAppLayoutApply.setOnClickListener {
            UserPreferences.appLayout = items
                .find { (_, view) -> view.ivSettingIsSelected.visibility == View.VISIBLE }
                ?.first
                ?: UserPreferences.AppLayout.AUTO

            if (UserPreferences.appLayout == UserPreferences.AppLayout.MOBILE) {
                (context as? MainTvActivity)?.finish()
                context.startActivity(Intent(context, MainMobileActivity::class.java))
            }

            hide()
        }


        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}