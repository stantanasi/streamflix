package com.tanasi.sflix.fragments.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.tanasi.sflix.databinding.FragmentPlayerBinding
import com.tanasi.sflix.models.Video


class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<PlayerFragmentArgs>()
    private val viewModel by viewModels<PlayerViewModel>()

    private lateinit var video: Video

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                PlayerViewModel.State.Loading -> {}
                is PlayerViewModel.State.SuccessLoading -> {
                    video = state.video
                    displayVideo()
                }
            }
        }

        viewModel.getVideo(args.linkId)
    }


    private fun displayVideo() {
    }
}