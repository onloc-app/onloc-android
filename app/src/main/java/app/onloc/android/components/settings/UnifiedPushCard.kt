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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.onloc.android.R
import org.unifiedpush.android.connector.UnifiedPush

private const val UNIFIED_PUSH_URL = "https://unifiedpush.org/"

@Composable
fun UnifiedPushCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var hasUnifiedPushProvider by remember { mutableStateOf(UnifiedPush.getDistributors(context).isNotEmpty()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasUnifiedPushProvider = UnifiedPush.getDistributors(context).isNotEmpty()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.permissions_unified_push_label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1.5f),
        )
        OutlinedButton(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, UNIFIED_PUSH_URL.toUri())) },
            enabled = !hasUnifiedPushProvider,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                if (hasUnifiedPushProvider) {
                    stringResource(R.string.permissions_registered_button_label)
                } else {
                    stringResource(R.string.permissions_learn_more_button_label)
                }
            )
        }
    }
}
