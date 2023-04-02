package com.tanasi.sflix.activities.launcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.utils.GitHub
import com.tanasi.sflix.utils.InAppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class LauncherViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.CheckingUpdate)
    val state: LiveData<State> = _state

    sealed class State {
        object CheckingUpdate : State()
        data class SuccessCheckingUpdate(val release: GitHub.Release?) : State()

        object DownloadingUpdate : State()
        data class SuccessDownloadingUpdate(val apk: File) : State()

        object InstallingUpdate : State()

        data class FailedUpdate(val error: Exception) : State()
    }

    init {
        checkUpdate()
    }


    private fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.CheckingUpdate)

        try {
            val release = InAppUpdater.getReleaseUpdate()

            _state.postValue(State.SuccessCheckingUpdate(release))
        } catch (e: Exception) {
            _state.postValue(State.FailedUpdate(e))
        }
    }
}