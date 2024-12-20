package ca.kebs.onloc.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnlocAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Onloc",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            text = "Log into an instance",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LoginForm()
                    }
                }
            }
        }
    }
}

@Composable
fun LoginForm() {
    var ip by remember { mutableStateOf("") }
    var isIpError by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isUsernameError by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordError by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("Server's IP") },
                singleLine = true,
                isError = isIpError.isNotEmpty(),
                supportingText = {
                    if (isIpError.isNotEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = isIpError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                isError = isUsernameError.isNotEmpty(),
                supportingText = {
                    if (isUsernameError.isNotEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = isUsernameError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            PasswordTextField(
                password = password,
                onPasswordChange = { password = it },
                isPasswordError = isPasswordError
            )

            Text(error, color = MaterialTheme.colorScheme.error)

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current
            Button(
                onClick = {
                    val isValid = validateInputs(
                        ip = ip,
                        username = username,
                        password = password,
                        onIpError = { isIpError = it },
                        onUsernameError = { isUsernameError = it },
                        onPasswordError = { isPasswordError = it }
                    )

                    error = ""

                    if (isValid) {
                        val authService = AuthService()
                        authService.login(ip, username, password, { token, user, errorMessage ->
                            if (token != null && user != null) {
                                Log.d("RESULT", "Token: $token, Id: ${user.id}, Username: ${user.username}")
                                val intent = Intent(context, LocationActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                            } else {
                                error = errorMessage ?: "Failure."
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login",)
            }
        }
    }
}

@Composable
fun PasswordTextField(
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordError: String
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        singleLine = true,
        visualTransformation =
        if (passwordVisible)
            VisualTransformation.None
        else
            PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (passwordVisible)
                Icons.Default.Visibility
            else
                Icons.Default.VisibilityOff

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = image,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        isError = isPasswordError.isNotEmpty(),
        supportingText = {
            if (isPasswordError.isNotEmpty()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = isPasswordError,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

fun validateInputs(
    ip: String,
    username: String,
    password: String,
    onIpError: (String) -> Unit,
    onUsernameError: (String) -> Unit,
    onPasswordError: (String) -> Unit
): Boolean {
    var isValid = true

    if (ip.isBlank()) {
        onIpError("IP is required")
        isValid = false
    } else {
        onIpError("")
    }

    if (username.isBlank()) {
        onUsernameError("Username is required")
        isValid = false
    } else {
        onUsernameError("")
    }

    if (password.isBlank()) {
        onPasswordError("Password is required")
        isValid = false
    } else {
        onPasswordError("")
    }

    return isValid
}
