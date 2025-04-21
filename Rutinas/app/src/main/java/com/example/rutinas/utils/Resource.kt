package com.example.rutinas.utils

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()  // Tipo explícito
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}