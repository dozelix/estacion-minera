package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.EncryptedAuthStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext_matchesAppName() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Estación Minera TI3042", appName)
  }

  @Test
  fun encryptedAuthStore_authenticatesDefaultDemoOperator() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authStore = EncryptedAuthStore(context)

    // Demo operator must be verifiable
    val profile = authStore.verifyCredentials(
      EncryptedAuthStore.DEFAULT_OPERATOR_EMAIL,
      EncryptedAuthStore.DEFAULT_OPERATOR_PASSWORD
    )

    assertNotNull("El operador de demostración debe estar presente y autenticado", profile)
    assertEquals(EncryptedAuthStore.DEFAULT_OPERATOR_EMAIL, profile!!.email)

    // Wrong password must be rejected
    val invalid = authStore.verifyCredentials(
      EncryptedAuthStore.DEFAULT_OPERATOR_EMAIL,
      "PasswordIncorrecta123"
    )
    assertNull("Una contraseña errónea debe ser rechazada", invalid)
  }

  @Test
  fun encryptedAuthStore_allowsRegistrationAndSessionManagement() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authStore = EncryptedAuthStore(context)

    val newEmail = "ingeniero.mina@faena.cl"
    val newPass = "SeguridadMinera2026!"
    val newName = "Ing. Roberto Soto"
    val newRole = "Jefe de Turno Chancado"

    val saved = authStore.saveUser(newEmail, newPass, newName, newRole)
    assertTrue("El usuario debe guardarse cifrado con éxito", saved)

    val user = authStore.verifyCredentials(newEmail, newPass)
    assertNotNull(user)
    assertEquals(newName, user!!.name)
    assertEquals(newRole, user.role)

    // Active session test
    authStore.setActiveSession(user)
    val active = authStore.getActiveSession()
    assertNotNull(active)
    assertEquals(newEmail, active!!.email)

    // Clear session test
    authStore.clearActiveSession()
    assertNull(authStore.getActiveSession())
  }
}
