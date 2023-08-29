package com.wild.smartrack.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.wild.smartrack.data.LoginCredentials
import com.wild.smartrack.data.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
): ViewModel() {
    var _uiState = MutableStateFlow(UiState.IDLE)
    var uiState: StateFlow<UiState>
        get() = _uiState
        set(value) {
            _uiState.value = value.value
        }

    private val _credentials = MutableStateFlow(LoginCredentials())
    val credentials: StateFlow<LoginCredentials> = _credentials

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    var email: String
        get() = _credentials.value.email
        set(value) {
            _credentials.value = _credentials.value.copy(email = value)
        }

    var password: String
        get() = _credentials.value.password
        set(value) {
            _credentials.value = _credentials.value.copy(password = value)
        }


    fun onSubmit() {
        val email = _credentials.value.email
        val password = _credentials.value.password

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginViewModel", "signInWithEmail:success")
                    _uiState.value = UiState.SUCCESS
                } else {
                    Log.w("LoginViewModel", "signInWithEmail:failure", task.exception)
                    _uiState.value = UiState.FAILURE
                    _errorMessage.value = "Authentication failed."
                }
            }
    }

}

