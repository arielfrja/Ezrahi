package com.arielfaridja.ezrahi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.domain.model.UserProfile
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.arielfaridja.ezrahi.util.logging.ErrorType
import com.arielfaridja.ezrahi.util.logging.ExceptionLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val passwordVerification: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth,
    private val logger: ExceptionLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun updateField(update: (SignUpUiState) -> SignUpUiState) {
        _uiState.value = update(_uiState.value)
    }

    fun signUp(onSuccess: (FirebaseUser) -> Unit) {
        val state = _uiState.value
        val error = validate(state)
        if (error != null) {
            _uiState.value = state.copy(errorMessage = error)
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(state.email.trim(), state.password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user != null) {
                        viewModelScope.launch {
                            repository.registerUser(
                                UserProfile(
                                    id = user.uid,
                                    firstName = state.firstName.trim(),
                                    lastName = state.lastName.trim(),
                                    email = state.email.trim(),
                                    phoneNumber = state.phone.trim()
                                )
                            ).onFailure {
                                logger.log(it, ErrorType.NETWORK, screen = "signup")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = it.localizedMessage ?: "Failed to save profile"
                                )
                                return@launch
                            }
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            onSuccess(user)
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Account creation failed"
                        )
                    }
                }
                .addOnFailureListener { err ->
                    logger.log(err, ErrorType.AUTH, screen = "signup")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Sign up failed"
                    )
                }
        }
    }

    private fun validate(state: SignUpUiState): String? = when {
        state.firstName.isBlank() -> "First name is required"
        state.lastName.isBlank() -> "Last name is required"
        state.email.isBlank() -> "Email is required"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() -> "Email is not valid"
        state.phone.isBlank() -> "Phone is required"
        state.password.length < 6 -> "Password must be at least 6 characters"
        state.password != state.passwordVerification -> "Passwords do not match"
        else -> null
    }
}