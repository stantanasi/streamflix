package com.tanasi.sflix.fragments.people

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.sflix.databinding.FragmentPeopleBinding

class PeopleFragment : Fragment() {

    private var _binding: FragmentPeopleBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<PeopleViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeopleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}