package com.tanasi.sflix.activities.launcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tanasi.sflix.utils.GitHub
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
}