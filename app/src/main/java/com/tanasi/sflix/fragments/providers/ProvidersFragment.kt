package com.tanasi.sflix.fragments.providers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentProvidersBinding
import com.tanasi.sflix.models.Provider
import com.tanasi.sflix.ui.SpacingItemDecoration

class ProvidersFragment : Fragment() {

    private var _binding: FragmentProvidersBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<ProvidersViewModel>()

    private val sflixAdapter = SflixAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProvidersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeProviders()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                ProvidersViewModel.State.Loading -> binding.isLoading.root.visibility = View.VISIBLE

                is ProvidersViewModel.State.SuccessLoading -> {
                    displayProviders(state.providers)
                    binding.isLoading.root.visibility = View.GONE
                }
                is ProvidersViewModel.State.FailedLoading -> {
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


    private fun initializeProviders() {
        binding.rvProviders.apply {
            adapter = sflixAdapter
            addItemDecoration(
                SpacingItemDecoration(
                    requireContext().resources.getDimension(R.dimen.providers_spacing).toInt()
                )
            )
        }
    }

    private fun displayProviders(providers: List<Provider>) {
        sflixAdapter.items.apply {
            clear()
            addAll(providers)
        }
        sflixAdapter.notifyDataSetChanged()

        binding.rvProviders.requestFocus()
    }
}