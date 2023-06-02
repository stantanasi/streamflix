package com.tanasi.streamflix.activities.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.utils.GitHub
import com.tanasi.streamflix.utils.InAppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel : ViewModel() {

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

    fun downloadUpdate(
        context: Context,
        asset: GitHub.Release.Asset,
    ) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.DownloadingUpdate)

        try {
            val apk = InAppUpdater.downloadApk(context, asset)

            _state.postValue(State.SuccessDownloadingUpdate(apk))
        } catch (e: Exception) {
            _state.postValue(State.FailedUpdate(e))
        }
    }

    fun installUpdate(
        context: Context,
        apk: File,
    ) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.InstallingUpdate)

        try {
            InAppUpdater.installApk(context, Uri.fromFile(apk))
        } catch (e: Exception) {
            _state.postValue(State.FailedUpdate(e))
        }
    }
}