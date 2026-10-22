package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Dimens
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.industrialStatusColors

/* =============================================================================
 * LoginScreen — acceso a la Estación de Supervisión.
 * Semántica (A1/A2):
 *  - Tokens de tema (dynamic scheme): correcto en modo claro y oscuro.
 *  - Insets propios (systemBars): sin huecos dobles ni contenido bajo barras.
 *  - Botón principal de 56 dp (≥ táctil) con máximo contraste.
 *  - Mensajes de error/éxito con doble codificación icono+color+texto.
 * ========================================================================== */
@Composable
fun LoginScreen(
  viewModel: LoginViewModel,
  onLoginSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val status = industrialStatusColors()
  var passwordVisible by remember { mutableStateOf(false) }

  if (uiState.isAuthenticated) {
    onLoginSuccess()
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = Dimens.Spacing24, vertical = Dimens.Spacing32),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Header: icono de marca + identificación del sistema
      Box(
        modifier = Modifier
          .size(Dimens.BrandBadge)
          .background(IndustrialAmber.copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraLarge),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = "Seguridad Minera",
          tint = IndustrialAmber,
          modifier = Modifier.size(Dimens.IconHero)
        )
      }

      Spacer(modifier = Modifier.height(Dimens.Spacing16))

      Text(
        text = "SISTEMA IoT MINERO TI3042",
        color = MaterialTheme.colorScheme.primary,   // ámbar/oscuro-ámbar según tema
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontFamily = FontFamily.Monospace
      )

      Text(
        text = "Estación de Supervisión",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Text(
        text = "Monitoreo Predictivo de Motor de Molienda",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(Dimens.Spacing16))

      // Security Badge (double coded: icon + label + description)
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(Dimens.Spacing12),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Key,
            contentDescription = "Acceso Seguro",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(Dimens.IconLg)
          )
          Spacer(modifier = Modifier.width(Dimens.Spacing10))
          Column {
            Text(
              text = "ACCESO AUTORIZADO",
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.secondary,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "Conexión segura y almacenamiento protegido de credenciales",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(Dimens.Spacing20))

      // Mode Tabs: Iniciar Sesión / Registrar Operador
      TabRow(
        selectedTabIndex = if (uiState.isRegistering) 1 else 0,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
      ) {
        Tab(
          selected = !uiState.isRegistering,
          onClick = { viewModel.toggleRegisterMode(false) },
          text = { Text("Iniciar Sesión", fontWeight = FontWeight.SemiBold) }
        )
        Tab(
          selected = uiState.isRegistering,
          onClick = { viewModel.toggleRegisterMode(true) },
          text = { Text("Registrar Operador", fontWeight = FontWeight.SemiBold) }
        )
      }

      Spacer(modifier = Modifier.height(Dimens.Spacing20))

      // Registration Name Field
      if (uiState.isRegistering) {
        OutlinedTextField(
          value = uiState.registerNameInput,
          onValueChange = viewModel::onRegisterNameChange,
          label = { Text("Nombre y Apellido del Operador") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_name_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = IndustrialAmber,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )
        Spacer(modifier = Modifier.height(Dimens.Spacing12))

        OutlinedTextField(
          value = uiState.registerRoleInput,
          onValueChange = viewModel::onRegisterRoleChange,
          label = { Text("Rol / Cargo en Faena") },
          leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_role_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = IndustrialAmber,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )
        Spacer(modifier = Modifier.height(Dimens.Spacing12))
      }

      // Email field
      OutlinedTextField(
        value = uiState.emailInput,
        onValueChange = viewModel::onEmailChange,
        label = { Text("Correo Electrónico de Faena") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("email_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = IndustrialAmber,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
      )

      Spacer(modifier = Modifier.height(Dimens.Spacing12))

      // Password field
      OutlinedTextField(
        value = uiState.passwordInput,
        onValueChange = viewModel::onPasswordChange,
        label = { Text("Contraseña de Acceso") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
            )
          }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("password_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = IndustrialAmber,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
      )

      // Feedback messages (doble codificación: icono + color + texto)
      if (uiState.errorMessage != null) {
        Spacer(modifier = Modifier.height(Dimens.Spacing10))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .background(status.danger.copy(alpha = 0.15f), MaterialTheme.shapes.small)
            .padding(Dimens.Spacing10)
        ) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = status.danger, modifier = Modifier.size(Dimens.Spacing18))
          Spacer(modifier = Modifier.width(Dimens.Spacing8))
          Text(text = uiState.errorMessage!!, color = status.danger, fontSize = 12.sp)
        }
      }

      if (uiState.successMessage != null) {
        Spacer(modifier = Modifier.height(Dimens.Spacing10))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .background(status.success.copy(alpha = 0.15f), MaterialTheme.shapes.small)
            .padding(Dimens.Spacing10)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = status.success, modifier = Modifier.size(Dimens.Spacing18))
          Spacer(modifier = Modifier.width(Dimens.Spacing8))
          Text(text = uiState.successMessage!!, color = status.success, fontSize = 12.sp)
        }
      }

      Spacer(modifier = Modifier.height(Dimens.Spacing20))

      // Submit Button (56dp touch target, máximo contraste)
      Button(
        onClick = {
          if (uiState.isRegistering) {
            viewModel.register()
          } else {
            viewModel.login()
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(Dimens.ButtonHeightLg)
          .testTag("login_submit_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = IndustrialAmber,   // alto contraste acro dark/light
          contentColor = Color.Black
        ),
        shape = MaterialTheme.shapes.medium
      ) {
        Text(
          text = if (uiState.isRegistering) "REGISTRAR OPERADOR" else "INGRESAR A SUPERVISIÓN",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          letterSpacing = 1.sp
        )
      }

      Spacer(modifier = Modifier.height(Dimens.Spacing14))

      // Demo button (52dp, táctil accesible)
      OutlinedButton(
        onClick = viewModel::fillDemoCredentials,
        modifier = Modifier
          .fillMaxWidth()
          .height(Dimens.ButtonHeight)
          .testTag("demo_credentials_button"),
        shape = MaterialTheme.shapes.medium
      ) {
        Text(
          text = "Cargar Credenciales de Demostración",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(Dimens.Spacing16))

      Text(
        text = "Sistema de Monitoreo Industrial de Faena • Minería 4.0",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}
