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

    private val sflixAdapter = SflixAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeopleBinding.inflate(inflater, container, false)
        viewModel.getPeopleById(args.id)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializePeople()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                PeopleViewModel.State.Loading -> binding.isLoading.root.visibility = View.VISIBLE

                is PeopleViewModel.State.SuccessLoading -> {
                    displayPeople(state.people)
                    binding.isLoading.root.visibility = View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun initializePeople() {
        binding.vgvPeople.apply {
            adapter = sflixAdapter
            setItemSpacing(80)
        }
    }

    private fun displayPeople(people: People) {
        sflixAdapter.items.apply {
            clear()
            add(people.apply { itemType = SflixAdapter.Type.PEOPLE })
        }
        sflixAdapter.notifyDataSetChanged()
    }
}