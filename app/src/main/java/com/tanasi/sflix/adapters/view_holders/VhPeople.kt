package com.tanasi.sflix.adapters.view_holders

import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ItemPeopleBinding
import com.tanasi.sflix.fragments.movie.MovieFragment
import com.tanasi.sflix.fragments.movie.MovieFragmentDirections
import com.tanasi.sflix.fragments.tv_show.TvShowFragment
import com.tanasi.sflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.sflix.models.People
import com.tanasi.sflix.utils.getCurrentFragment
import com.tanasi.sflix.utils.toActivity

class VhPeople(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var people: People

    fun bind(people: People) {
        this.people = people

        when (_binding) {
            is ItemPeopleBinding -> displayPeople(_binding)
        }
    }


    private fun displayPeople(binding: ItemPeopleBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is MovieFragment -> findNavController().navigate(
                        MovieFragmentDirections.actionMovieToPeople(people.slug)
                    )
                    is TvShowFragment -> findNavController().navigate(
                        TvShowFragmentDirections.actionTvShowToPeople(people.slug)
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