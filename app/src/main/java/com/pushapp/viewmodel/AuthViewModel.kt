package com.pushapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pushapp.model.User
import com.pushapp.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoggedIn = MutableStateFlow(repository.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        if (repository.isLoggedIn) {
            viewModelScope.launch {
                _currentUser.value = repository.getCurrentUser()
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.login(username.trim(), password).fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(friendlyError(e.message))
                }
            )
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.register(username.trim(), password).fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(friendlyError(e.message))
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        _isLoggedIn.value = false
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun friendlyError(msg: String?): String = when {
        msg == null -> "Неизвестная ошибка"
        msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("wrong-password") || msg.contains("user-not-found") ->
            "Неверный логин или пароль"
        msg.contains("email-already-in-use") -> "Этот логин уже занят"
        msg.contains("weak-password") -> "Пароль слишком короткий (минимум 6 символов)"
        msg.contains("network") -> "Нет подключения к сети"
        else -> msg
    }
}
