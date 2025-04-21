package com.example.rutinas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
open // ✅ Anotación requerida
class BaseViewModel @Inject constructor() : ViewModel() {
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private fun handleError(error: Throwable) {
        _errorMessage.postValue(error.message ?: "Error desconocido")
        Timber.e(error)
    }
}