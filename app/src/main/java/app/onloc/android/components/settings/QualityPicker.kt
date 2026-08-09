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

import android.location.LocationRequest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.onloc.android.R

@Composable
fun QualityPicker(
    quality: Int,
    onQualityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(modifier = modifier) {
        val options = arrayOf(
            LocationRequest.QUALITY_HIGH_ACCURACY,
            LocationRequest.QUALITY_BALANCED_POWER_ACCURACY,
            LocationRequest.QUALITY_LOW_POWER,
        )
        MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    checked = option == quality,
                    onCheckedChange = { onQualityChange(option) },
                    label = {
                        Text(
                            when (option) {
                                LocationRequest.QUALITY_HIGH_ACCURACY ->
                                    stringResource(R.string.quality_high_accuracy)

                                LocationRequest.QUALITY_BALANCED_POWER_ACCURACY ->
                                    stringResource(R.string.quality_balance_power_accuracy)

                                else ->
                                    stringResource(R.string.quality_low_power)
                            },
                        )
                    },
                    enabled = enabled,
                    icon = {},
                )
            }
        }
    }
}
