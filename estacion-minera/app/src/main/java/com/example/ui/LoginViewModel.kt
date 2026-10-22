package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.security.EncryptedAuthStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LoginUiState(
  val emailInput: String = EncryptedAuthStore.DEFAULT_OPERATOR_EMAIL,
  val passwordInput: String = EncryptedAuthStore.DEFAULT_OPERATOR_PASSWORD,
  val registerNameInput: String = "",
  val registerRoleInput: String = "Operador de Planta",
  val isRegistering: Boolean = false,
  val isAuthenticated: Boolean = false,
  val activeUser: EncryptedAuthStore.UserProfile? = null,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

  private val authStore = EncryptedAuthStore(application)

  private val _uiState = MutableStateFlow(
    LoginUiState(
      activeUser = authStore.getActiveSession(),
      isAuthenticated = authStore.getActiveSession() != null
    )
  )
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  fun onEmailChange(value: String) {
    _uiState.value = _uiState.value.copy(emailInput = value, errorMessage = null)
  }

  fun onPasswordChange(value: String) {
    _uiState.value = _uiState.value.copy(passwordInput = value, errorMessage = null)
  }

  fun onRegisterNameChange(value: String) {
    _uiState.value = _uiState.value.copy(registerNameInput = value, errorMessage = null)
  }

  fun onRegisterRoleChange(value: String) {
    _uiState.value = _uiState.value.copy(registerRoleInput = value, errorMessage = null)
  }

  fun toggleRegisterMode(isRegister: Boolean) {
    _uiState.value = _uiState.value.copy(
      isRegistering = isRegister,
      errorMessage = null,
      successMessage = null
    )
  }

  fun fillDemoCredentials() {
    _uiState.value = _uiState.value.copy(
      emailInput = EncryptedAuthStore.DEFAULT_OPERATOR_EMAIL,
      passwordInput = EncryptedAuthStore.DEFAULT_OPERATOR_PASSWORD,
      errorMessage = null,
      successMessage = "Credenciales demo cargadas"
    )
  }

  fun login() {
    val state = _uiState.value
    if (state.emailInput.isBlank() || state.passwordInput.isBlank()) {
      _uiState.value = state.copy(errorMessage = "Ingrese correo de faena y contraseña")
      return
    }

    val user = authStore.verifyCredentials(state.emailInput, state.passwordInput)
    if (user != null) {
      authStore.setActiveSession(user)
      _uiState.value = state.copy(
        isAuthenticated = true,
        activeUser = user,
        errorMessage = null,
        successMessage = null
      )
    } else {
      _uiState.value = state.copy(
        errorMessage = "Credenciales incorrectas o usuario no registrado"
      )
    }
  }

  fun register() {
    val state = _uiState.value
    if (state.emailInput.isBlank() || state.passwordInput.isBlank() || state.registerNameInput.isBlank()) {
      _uiState.value = state.copy(errorMessage = "Complete todos los campos de registro")
      return
    }
    if (state.passwordInput.length < 8) {
      _uiState.value = state.copy(errorMessage = "La contraseña requiere un mínimo de 8 caracteres")
      return
    }

    val saved = authStore.saveUser(
      email = state.emailInput,
      password = state.passwordInput,
      name = state.registerNameInput,
      role = state.registerRoleInput.ifBlank { "Operador de Planta" }
    )

    if (saved) {
      val user = authStore.verifyCredentials(state.emailInput, state.passwordInput)
      authStore.setActiveSession(user)
      _uiState.value = state.copy(
        isAuthenticated = true,
        activeUser = user,
        isRegistering = false,
        errorMessage = null,
        successMessage = "Usuario registrado con cifrado AES-256 GCM exitosamente"
      )
    } else {
      _uiState.value = state.copy(errorMessage = "Error al cifrar y guardar en almacenamiento seguro")
    }
  }

  fun logout() {
    authStore.clearActiveSession()
    _uiState.value = _uiState.value.copy(
      isAuthenticated = false,
      activeUser = null,
      errorMessage = null,
      successMessage = null
    )
  }
}
