package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StatusPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DevelopmentalRadarChart(
    scores: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val animatedValues = scores.map { (_, v) ->
        val anim by animateFloatAsState(
            targetValue = (v.coerceIn(0, 100) / 100f),
            animationSpec = tween(durationMillis = 600),
            label = "radar"
        )
        anim
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val count = scores.size.coerceAtLeast(3)
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) * 0.85f
                val stepAngle = (2.0 * PI) / count

                // Concentric web rings
                listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { ring ->
                    val ringPath = Path()
                    for (i in 0 until count) {
                        val angle = -PI / 2.0 + i * stepAngle
                        val x = center.x + (radius * ring * cos(angle)).toFloat()
                        val y = center.y + (radius * ring * sin(angle)).toFloat()
                        if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                    }
                    ringPath.close()
                    drawPath(ringPath, color = gridColor, style = Stroke(width = 1.5f))
                }

                // Axes spokes
                for (i in 0 until count) {
                    val angle = -PI / 2.0 + i * stepAngle
                    val x = center.x + (radius * cos(angle)).toFloat()
                    val y = center.y + (radius * sin(angle)).toFloat()
                    drawLine(gridColor, start = center, end = Offset(x, y), strokeWidth = 1.5f)
                }

                // Data polygon
                val dataPath = Path()
                val vertices = mutableListOf<Offset>()
                for (i in 0 until count) {
                    val angle = -PI / 2.0 + i * stepAngle
                    val v = animatedValues.getOrElse(i) { 0f }
                    val x = center.x + (radius * v * cos(angle)).toFloat()
                    val y = center.y + (radius * v * sin(angle)).toFloat()
                    vertices.add(Offset(x, y))
                    if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                }
                dataPath.close()
                drawPath(dataPath, color = primaryColor.copy(alpha = 0.25f))
                drawPath(dataPath, color = primaryColor, style = Stroke(width = 4f, cap = StrokeCap.Round))

                vertices.forEach { pt ->
                    drawCircle(color = accentColor, radius = 6f, center = pt)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        scores.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(120.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    ) {
                        drawRoundRect(
                            color = gridColor.copy(alpha = 0.45f),
                            cornerRadius = CornerRadius(8f, 8f),
                            size = size
                        )
                        drawRoundRect(
                            color = primaryColor,
                            cornerRadius = CornerRadius(8f, 8f),
                            size = Size(size.width * (value.coerceIn(0, 100) / 100f), size.height)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$value%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }
    }
}

@Composable
fun ServiceBreakdownBarChart(
    items: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxVal = (items.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val barPrimary = MaterialTheme.colorScheme.primary
    val barSecondary = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { index, (title, count) ->
            val ratio = count.toFloat() / maxVal.toFloat()
            val color = if (index % 2 == 0) barPrimary else barSecondary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(110.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                    ) {
                        drawRoundRect(
                            color = trackColor,
                            cornerRadius = CornerRadius(8f, 8f),
                            size = size
                        )
                        drawRoundRect(
                            color = color,
                            cornerRadius = CornerRadius(8f, 8f),
                            size = Size(size.width * ratio.coerceIn(0.05f, 1f), size.height)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
