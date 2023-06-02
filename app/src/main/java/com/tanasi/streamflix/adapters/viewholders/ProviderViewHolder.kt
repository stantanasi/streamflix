package com.tanasi.streamflix.adapters.viewholders

import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.ItemProviderBinding
import com.tanasi.streamflix.models.Provider
import com.tanasi.streamflix.utils.UserPreferences

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
                findNavController().navigate(
                    resId = R.id.home,
                    args = null,
                    navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.providers, true)
                        .build()
                )
            }
        }

        Glide.with(context)
            .load(provider.logo)
            .into(binding.ivProviderLogo)

        binding.tvProviderName.text = provider.name
    }
}