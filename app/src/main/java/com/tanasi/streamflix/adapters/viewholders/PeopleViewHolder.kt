package com.tanasi.streamflix.adapters.viewholders

import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.ItemPeopleBinding
import com.tanasi.streamflix.fragments.movie.MovieFragment
import com.tanasi.streamflix.fragments.movie.MovieFragmentDirections
import com.tanasi.streamflix.fragments.tv_show.TvShowFragment
import com.tanasi.streamflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity

class PeopleViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var people: People

    fun bind(people: People) {
        this.people = people

        when (_binding) {
            is ItemPeopleBinding -> displayItem(_binding)
        }
    }


    private fun displayItem(binding: ItemPeopleBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is MovieFragment -> findNavController().navigate(
                        MovieFragmentDirections.actionMovieToPeople(people.id)
                    )
                    is TvShowFragment -> findNavController().navigate(
                        TvShowFragmentDirections.actionTvShowToPeople(people.id)
                    )
                }
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

        binding.ivPeopleImage.apply {
            clipToOutline = true
            Glide.with(context)
                .load(people.image)
                .placeholder(R.drawable.ic_person_placeholder)
                .centerCrop()
                .into(this)
        }

        binding.tvPeopleName.text = people.name
    }
}