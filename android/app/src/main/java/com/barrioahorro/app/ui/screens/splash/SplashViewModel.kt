package com.barrioahorro.app.ui.screens.splash

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barrioahorro.app.data.local.datastore.AuthTokenDataStore
import com.barrioahorro.app.domain.model.SessionRefreshResult
import com.barrioahorro.app.domain.model.UserType
import com.barrioahorro.app.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import javax.inject.Inject

sealed interface SplashDestination {
    /** No hay sesión (o venció): ir a la selección de rol. */
    data object Auth : SplashDestination

    /** Hay sesión guardada: entrar directo. */
    data class Home(val userType: UserType) : SplashDestination
}

private const val REFRESH_TIMEOUT_MILLIS = 3_000L

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenStore: AuthTokenDataStore,
    private val authRepository: IAuthRepository,
) : ViewModel() {

    // null = todavía resolviendo (el splash sigue en pantalla)
    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch { _destination.value = resolveDestination() }
    }

    private suspend fun resolveDestination(): SplashDestination {
        val token = tokenStore.currentToken() ?: return SplashDestination.Auth

        val userType = tokenStore.currentUserType()
            ?.let { runCatching { UserType.valueOf(it) }.getOrNull() }
            ?: return SplashDestination.Auth

        if (isExpired(token)) {
            tokenStore.clearSession()
            return SplashDestination.Auth
        }

        // Renovamos el token por otras 24 h. Si no hay conexión, entramos igual con el actual.
        val refresh = withTimeoutOrNull(REFRESH_TIMEOUT_MILLIS) { authRepository.refreshSession() }
            ?: SessionRefreshResult.UNREACHABLE

        if (refresh == SessionRefreshResult.REJECTED) {
            tokenStore.clearSession()
            return SplashDestination.Auth
        }

        return SplashDestination.Home(userType)
    }

    /**
     * Lectura local del claim "exp" del JWT, solo para no entrar con un token vencido.
     * No valida la firma (eso lo hace el servidor). Si no se puede leer, deja decidir al servidor.
     */
    private fun isExpired(token: String): Boolean = runCatching {
        val payload = token.split(".")[1]
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        JSONObject(json).getLong("exp") <= System.currentTimeMillis() / 1000
    }.getOrDefault(false)
}