package dev.csse.pfnassar.daytripper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.csse.pfnassar.daytripper.data.RouteRepository

class DTViewModelFactory(
    private val routeRepository: RouteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DTViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DTViewModel(routeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
