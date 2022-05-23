package com.tanasi.sflix.fragments.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.leanback.app.VideoSupportFragment
import androidx.navigation.fragment.navArgs
import com.tanasi.sflix.databinding.FragmentPlayerBinding
import com.tanasi.sflix.models.Video


class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<PlayerFragmentArgs>()
    private val viewModel by viewModels<PlayerViewModel>()

    private val videoFragment = VideoSupportFragment()
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

        parentFragmentManager
            .beginTransaction()
            .replace(binding.flPlayer.id, videoFragment)
            .commit()
    }
}