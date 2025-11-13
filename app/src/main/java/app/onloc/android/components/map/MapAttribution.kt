/*
 * Copyright (C) 2025 Thomas Lavoie
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

package app.onloc.android.components.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun MapAttribution(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                withLink(
                    LinkAnnotation.Url(
                        "https://maplibre.org/",
                        TextLinkStyles(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                            )
                        ),
                    ),
                ) {
                    append("MapLibre")
                }
            }
        )
        VerticalDivider(modifier = Modifier.fillMaxHeight())
        Text(
            buildAnnotatedString {
                withLink(
                    LinkAnnotation.Url(
                        "https://immich.app/",
                        TextLinkStyles(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                            )
                        ),
                    ),
                ) {
                    append("Immich")
                }
            }
        )
        VerticalDivider(modifier = Modifier.fillMaxHeight())
        Text(
            buildAnnotatedString {
                withLink(
                    LinkAnnotation.Url(
                        "https://www.openstreetmap.org/copyright/",
                        TextLinkStyles(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                            )
                        ),
                    ),
                ) {
                    append("© OpenStreetMap")
                }
            }
        )
    }
}
