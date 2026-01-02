package app.onloc.android.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import app.onloc.android.R

@Composable
fun PasswordTextField(
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordError: String,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(R.string.login_password_field_label)) },
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
        modifier = modifier.fillMaxWidth()
    )
}
