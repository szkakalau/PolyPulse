package com.polypulse.app.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.polypulse.app.data.remote.dto.PricePointDto

@Composable
fun SimpleLineChart(
    data: List<PricePointDto>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Green,
    lineWidth: Float = 4f
) {
    if (data.isEmpty()) return

    val sortedData = data.sortedBy { it.t }
    val minPrice = sortedData.minOf { it.p }
    val maxPrice = sortedData.maxOf { it.p }
    val minTime = sortedData.minOf { it.t }
    val maxTime = sortedData.maxOf { it.t }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Add some padding
        val padding = 16.dp.toPx()
        val graphWidth = width
        val graphHeight = height - (padding * 2)

        val path = Path()
        
        sortedData.forEachIndexed { index, point ->
            // Normalize X and Y
            val x = ((point.t - minTime).toFloat() / (maxTime - minTime)) * graphWidth
            // Flip Y because Canvas (0,0) is top-left
            val y = height - padding - (((point.p - minPrice).toFloat() / (maxPrice - minPrice).coerceAtLeast(0.01)) * graphHeight)
            
            if (index == 0) {
                path.moveTo(x, y.toFloat())
            } else {
                path.lineTo(x, y.toFloat())
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = lineWidth)
        )
    }
}
