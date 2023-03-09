package com.tanasi.sflix.adapters.viewholders

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.activities.MainActivity
import com.tanasi.sflix.databinding.ItemProviderBinding
import com.tanasi.sflix.models.Provider
import com.tanasi.sflix.utils.AppPreferences
import com.tanasi.sflix.utils.toActivity

class ProviderViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var provider: Provider

    fun bind(provider: Provider) {
        this.provider = provider

        when (_binding) {
            is ItemProviderBinding -> displayItem(_binding)
        }
    }


    private fun displayItem(binding: ItemProviderBinding) {
        binding.root.apply {
            setOnClickListener {
                AppPreferences.currentProvider = provider.provider
                context.startActivity(Intent(context, MainActivity::class.java))
                context.toActivity()?.finish()
            }
        }

        Glide.with(context)
            .load(provider.logo)
            .into(binding.ivProviderLogo)

        binding.tvProviderName.text = provider.name
    }
}