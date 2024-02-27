package com.tanasi.streamflix.fragments.providers

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.Provider
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProvidersViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val providers: List<Provider>) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getProviders()
    }


    fun getProviders() = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val providers = UserPreferences.providers.map {
                Provider(
                    name = it.name,
                    logo = it.logo,

                    provider = it,
                )
            }.sortedBy { it.name }

            _state.postValue(State.SuccessLoading(providers))
        } catch (e: Exception) {
            Log.e("ProvidersViewModel", "getProviders: ", e)
            _state.postValue(State.FailedLoading(e))
        }
    }
}