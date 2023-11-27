package com.tanasi.streamflix.adapters.viewholders

import android.content.Intent
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.streamflix.activities.main.MainActivity
import com.tanasi.streamflix.databinding.ItemProviderBinding
import com.tanasi.streamflix.fragments.providers.ProvidersFragmentDirections
import com.tanasi.streamflix.models.Provider
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.toActivity

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
                UserPreferences.currentProvider = provider.provider
                context.toActivity()?.apply {
                    finish()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        }

        Glide.with(context)
            .load(provider.logo)
            .into(binding.ivProviderLogo)

        binding.tvProviderName.text = provider.name
    }
}