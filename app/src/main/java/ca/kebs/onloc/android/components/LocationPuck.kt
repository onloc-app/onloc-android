package ca.kebs.onloc.android.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.layer.SymbolLayer
import dev.sargunv.maplibrecompose.core.source.Source
import dev.sargunv.maplibrecompose.expressions.dsl.const
import dev.sargunv.maplibrecompose.expressions.dsl.format
import dev.sargunv.maplibrecompose.expressions.dsl.offset
import dev.sargunv.maplibrecompose.expressions.dsl.span

@Composable
fun LocationPuck(
    id: Int,
    source: Source,
    metersPerDp: Double,
    color: Color,
    name: String? = null,
    accuracy: Double? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    accuracy?.let {
        CircleLayer(
            id = "location-accuracy-$id",
            source = source,
            radius = const((accuracy / metersPerDp).dp),
            opacity = const(.5f),
            color = const(color),
        )
    }
    CircleLayer(
        id = "location-puck-$id",
        source = source,
        radius = const(10.dp),
        strokeWidth = const(2.dp),
        strokeColor = const(Color.White),
        color = const(color),
        onClick = {
            onClick()
            ClickResult.Consume
        },
        onLongClick = {
            onLongClick()
            ClickResult.Consume
        })
    name?.let {
        val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
        SymbolLayer(
            id = "location-device-name-$id",
            source = source,
            textField =
                format(
                    span(const(name), textSize = const(1.2f.em)),
                ),
            textFont = const(listOf("Noto Sans Regular")),
            textSize = const(1.2f.em),
            textColor = const(textColor),
            textOffset = offset(0f.em, (1.5f).em),
            onClick = {
                onClick()
                ClickResult.Consume
            },
        )
    }
}