package com.barrioahorro.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException


@HiltViewModel
class SessionViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    fun logout(onLoggedOut: () -> Unit) {
        if (_isLoggingOut.value) return

        _isLoggingOut.value = true
        viewModelScope.launch {
            try {
                logoutUseCase()
                onLoggedOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isLoggingOut.value = false
            }
        }
    }
}