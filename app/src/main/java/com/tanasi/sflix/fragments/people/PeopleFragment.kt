package com.tanasi.sflix.fragments.people

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentPeopleBinding
import com.tanasi.sflix.models.People

class PeopleFragment : Fragment() {

    private var _binding: FragmentPeopleBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<PeopleFragmentArgs>()
    private val viewModel by viewModels<PeopleViewModel>()

    private lateinit var people: People

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentPeopleBinding.inflate(inflater, container, false)
            viewModel.fetchCast(args.slug)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                PeopleViewModel.State.Loading -> {}

                is PeopleViewModel.State.SuccessLoading -> {
                    people = state.people
                    displayPeople()
                }
                is PeopleViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun displayPeople() {
        binding.vgvPeople.apply {
            adapter = SflixAdapter(mutableListOf<SflixAdapter.Item>().also {
                it.add(people.apply { itemType = SflixAdapter.Type.PEOPLE_HEADER })
            })
            setItemSpacing(80)
        }
    }
}