package com.tanasi.sflix.adapters.viewholders

import android.graphics.drawable.GradientDrawable
import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ItemGenreGridBinding
import com.tanasi.sflix.fragments.search.SearchFragmentDirections
import com.tanasi.sflix.models.Genre


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
            is ItemGenreGridBinding -> displayGridItem(_binding)
        }
    }


    private fun displayGridItem(binding: ItemGenreGridBinding) {
        binding.root.apply {
            val colors = context.resources.getIntArray(R.array.genres)
            (background as? GradientDrawable)?.setColor(colors[bindingAdapterPosition % colors.size])

            setOnClickListener {
                findNavController().navigate(
                    SearchFragmentDirections.actionSearchToGenre(
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