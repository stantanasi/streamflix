package com.tanasi.sflix.adapters.view_holders

import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.ContentPeopleBinding
import com.tanasi.sflix.databinding.ItemPeopleBinding
import com.tanasi.sflix.fragments.movie.MovieFragment
import com.tanasi.sflix.fragments.movie.MovieFragmentDirections
import com.tanasi.sflix.fragments.tv_show.TvShowFragment
import com.tanasi.sflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.People
import com.tanasi.sflix.models.TvShow
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
            is ItemPeopleBinding -> displayItem(_binding)

            is ContentPeopleBinding -> displayPeople(_binding)
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


    private fun displayPeople(binding: ContentPeopleBinding) {
        binding.tvPeopleName.text = people.name

        binding.ivPeopleImage.apply {
            clipToOutline = true
            Glide.with(context)
                .load(people.image)
                .placeholder(R.drawable.ic_person_placeholder)
                .centerCrop()
                .into(this)
        }

        binding.hgvPeopleFilmography.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(people.filmography.onEach {
                when (it) {
                    is Movie -> it.itemType = SflixAdapter.Type.MOVIE_ITEM
                    is TvShow -> it.itemType = SflixAdapter.Type.TV_SHOW_ITEM
                }
            })
            setItemSpacing(20)
        }
    }
}