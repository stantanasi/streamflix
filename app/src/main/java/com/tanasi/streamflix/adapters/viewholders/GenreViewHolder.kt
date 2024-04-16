package com.tanasi.streamflix.adapters.viewholders

import android.graphics.drawable.GradientDrawable
import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.ItemGenreGridMobileBinding
import com.tanasi.streamflix.databinding.ItemGenreGridTvBinding
import com.tanasi.streamflix.fragments.search.SearchMobileFragmentDirections
import com.tanasi.streamflix.fragments.search.SearchTvFragmentDirections
import com.tanasi.streamflix.models.Genre


class GenreViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var genre: Genre

    fun bind(genre: Genre) {
        this.genre = genre

        when (_binding) {
            is ItemGenreGridMobileBinding -> displayGridMobileItem(_binding)
            is ItemGenreGridTvBinding -> displayGridTvItem(_binding)
        }
    }


    private fun displayGridMobileItem(binding: ItemGenreGridMobileBinding) {
        binding.root.apply {
            val colors = context.resources.getIntArray(R.array.genres)
            (background as? GradientDrawable)?.setColor(colors[bindingAdapterPosition % colors.size])

            setOnClickListener {
                findNavController().navigate(
                    SearchMobileFragmentDirections.actionSearchToGenre(
                        id = genre.id,
                        name = genre.name,
                    )
                )
            }
        }

        binding.tvGenreName.text = genre.name
    }

    private fun displayGridTvItem(binding: ItemGenreGridTvBinding) {
        binding.root.apply {
            val colors = context.resources.getIntArray(R.array.genres)
            (background as? GradientDrawable)?.setColor(colors[bindingAdapterPosition % colors.size])

            setOnClickListener {
                findNavController().navigate(
                    SearchTvFragmentDirections.actionSearchToGenre(
                        id = genre.id,
                        name = genre.name,
                    )
                )
            }
            setOnFocusChangeListener { _, hasFocus ->
                val animation = when {
                    hasFocus -> AnimationUtils.loadAnimation(context, R.anim.zoom_in)
                    else -> AnimationUtils.loadAnimation(context, R.anim.zoom_out)
                }
                binding.root.startAnimation(animation)
                animation.fillAfter = true
            }
        }

        binding.tvGenreName.text = genre.name
    }
}