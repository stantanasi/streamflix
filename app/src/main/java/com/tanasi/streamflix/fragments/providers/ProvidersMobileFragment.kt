package com.tanasi.streamflix.fragments.providers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentProvidersMobileBinding
import com.tanasi.streamflix.models.Provider
import com.tanasi.streamflix.providers.Provider.Companion.providers
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.dp
import kotlinx.coroutines.launch
import java.util.Locale

class ProvidersMobileFragment : Fragment() {

    private var _binding: FragmentProvidersMobileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<ProvidersViewModel>()

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProvidersMobileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeProviders()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { state ->
                when (state) {
                    ProvidersViewModel.State.Loading -> binding.isLoading.apply {
                        root.visibility = View.VISIBLE
                        pbIsLoading.visibility = View.VISIBLE
                        gIsLoadingRetry.visibility = View.GONE
                    }
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
                        binding.isLoading.apply {
                            pbIsLoading.visibility = View.GONE
                            gIsLoadingRetry.visibility = View.VISIBLE
                            btnIsLoadingRetry.setOnClickListener {
                                viewModel.getProviders()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun initializeProviders() {
        binding.sProvidersLanguage.apply {
            class Language(
                val code: String,
                val name: String,
            )

            val languages = providers
                .distinctBy { it.language }
                .map {
                    val locale = Locale(it.language)

                    Language(
                        code = it.language,
                        name = locale.getDisplayLanguage(locale)
                            .replaceFirstChar { char -> char.titlecase() },
                    )
                }
                .sortedBy { it.name.lowercase() }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position == 0) {
                        viewModel.getProviders()
                        UserPreferences.currentLanguage = null
                    } else {
                        viewModel.getProviders(languages[position - 1].code)
                        UserPreferences.currentLanguage = languages[position - 1].code
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

            val spinnerAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languages.map { it.name }.toMutableList()
                    .also { it.add(0, context.getString(R.string.providers_all_languages)) }
                    .toTypedArray()
            ).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setAdapter(spinnerAdapter)

            setSelection(
                UserPreferences.currentLanguage?.let {
                    languages.indexOfFirst { language -> language.code == it } + 1
                } ?: 0
            )
        }

        binding.rvProviders.apply {
            adapter = appAdapter.apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            addItemDecoration(
                SpacingItemDecoration(32.dp(requireContext()))
            )
        }
    }

    private fun displayProviders(providers: List<Provider>) {
        appAdapter.submitList(providers.onEach {
            it.itemType = AppAdapter.Type.PROVIDER_MOBILE_ITEM
        })
    }
}