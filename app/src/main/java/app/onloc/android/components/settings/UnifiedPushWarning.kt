/*
 * Copyright (C) 2026 Thomas Lavoie
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package app.onloc.android.components.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.onloc.android.R
import app.onloc.android.services.ServiceManager
import app.onloc.android.services.connection.WebSocketConnectionStrategy

private const val UNIFIED_PUSH_URL = "https://unifiedpush.org/"

@Composable
fun UnifiedPushWarning(modifier: Modifier = Modifier) {
    if (ServiceManager.connectionStrategy !is WebSocketConnectionStrategy) return

    val context = LocalContext.current

    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                Text(
                    text = stringResource(R.string.settings_dialog_unified_push_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(text = stringResource(R.string.settings_dialog_unified_push_description))
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, UNIFIED_PUSH_URL.toUri()))
                },
            ) {
                Text(text = stringResource(R.string.settings_dialog_unified_push_button))
            }
        }
    }
}
