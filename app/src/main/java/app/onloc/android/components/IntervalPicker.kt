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

package app.onloc.android.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import app.onloc.android.R
import app.onloc.android.ServicePreferences
import kotlin.math.roundToInt

private data class TimeStep(
    val multiplier: Int,
    val bounds: IntRange,
    val steps: Int,
    val stepSize: Int,
    val pluralResId: Int,
)

private val timeSteps = arrayOf(
    TimeStep(
        multiplier = 1,
        bounds = 5..60,
        steps = 12 - 2,
        stepSize = 5,
        pluralResId = R.plurals.interval_picker_seconds,
    ),
    TimeStep(
        multiplier = 60,
        bounds = 5..60,
        steps = 12 - 2,
        stepSize = 5,
        pluralResId = R.plurals.interval_picker_minutes,
    ),
    TimeStep(
        multiplier = 3600,
        bounds = 1..24,
        steps = 24 - 2,
        stepSize = 1,
        pluralResId = R.plurals.interval_picker_hours,
    ),
    TimeStep(
        multiplier = 86400,
        bounds = 1..7,
        steps = 7 - 2,
        stepSize = 1,
        pluralResId = R.plurals.interval_picker_days,
    )
)

@Composable
fun IntervalPicker(
    value: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onChange: (value: Int) -> Unit = {},
) {
    val context = LocalContext.current
    val servicePreferences = ServicePreferences(context)

    fun updateInterval(newValue: Int) {
        onChange(newValue)
        servicePreferences.createLocationUpdatesInterval(newValue)
    }

    val bestOptionIndex = remember(value) {
        timeSteps.indexOfLast {
            value >= it.multiplier
        }.coerceAtLeast(0)
    }
    var selectedOption by remember { mutableStateOf(timeSteps[bestOptionIndex]) }

    Column(modifier = modifier) {
        MultiChoiceSegmentedButtonRow {
            timeSteps.forEachIndexed { index, timeStep ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = timeSteps.size
                    ),
                    checked = timeStep == selectedOption,
                    onCheckedChange = {
                        selectedOption = timeStep
                        updateInterval(timeStep.bounds.first * timeStep.multiplier)
                    },
                    label = {
                        Text(pluralStringResource(timeStep.pluralResId, value / selectedOption.multiplier))
                    },
                    enabled = enabled,
                )
            }
        }

        Slider(
            value = (value / selectedOption.multiplier).coerceIn(1, selectedOption.bounds.last).toFloat(),
            onValueChange = {
                val snapped = (it / selectedOption.stepSize).roundToInt() * selectedOption.stepSize
                updateInterval(snapped * selectedOption.multiplier)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTickColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            steps = selectedOption.steps.coerceAtLeast(0),
            valueRange = selectedOption.bounds.first.toFloat()..selectedOption.bounds.last.toFloat(),
            enabled = enabled,
        )
        Text(
            text = "${(value / selectedOption.multiplier)} ${
                pluralStringResource(selectedOption.pluralResId, value / selectedOption.multiplier)
            }"
        )
    }
}
