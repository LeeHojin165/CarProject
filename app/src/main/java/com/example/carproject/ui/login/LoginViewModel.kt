package com.example.carproject.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LoginUiState(
    val profileName: String = "[프로필 이름]",
    val manualCode: String = "",
    val remainingText: String = "",
)

class LoginViewModel(auth: AuthRepository) : ViewModel() {
    val uiState: StateFlow<LoginUiState> = auth.startQrLogin()
        .map { s ->
            LoginUiState(
                profileName = s.profileName,
                manualCode = s.manualCode,
                remainingText = "%d:%02d 남음".format(s.remainingSeconds / 60, s.remainingSeconds % 60),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoginUiState())

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { LoginViewModel(c.authRepository) } }
    }
}
