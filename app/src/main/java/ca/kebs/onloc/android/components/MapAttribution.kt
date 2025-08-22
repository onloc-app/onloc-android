package ca.kebs.onloc.android.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

@Composable
fun MapAttribution() {
    Row(
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