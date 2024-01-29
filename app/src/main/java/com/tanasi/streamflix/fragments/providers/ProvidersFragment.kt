package com.tanasi.streamflix.fragments.providers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentProvidersBinding
import com.tanasi.streamflix.models.Provider
import com.tanasi.streamflix.ui.SpacingItemDecoration

class ProvidersFragment : Fragment() {

    private var _binding: FragmentProvidersBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<ProvidersViewModel>()

    private val appAdapter = AppAdapter()

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
                        state.error.message ?: "",
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
            adapter = appAdapter
            addItemDecoration(
                SpacingItemDecoration(
                    requireContext().resources.getDimension(R.dimen.providers_spacing).toInt()
                )
            )
        }
    }

    private fun displayProviders(providers: List<Provider>) {
        appAdapter.submitList(providers)

        binding.rvProviders.requestFocus()
    }
}