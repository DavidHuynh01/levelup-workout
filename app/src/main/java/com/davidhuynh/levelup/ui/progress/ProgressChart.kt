package com.davidhuynh.levelup.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.davidhuynh.levelup.domain.logic.ProgressPoint
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.ui.theme.ChartLine
import com.davidhuynh.levelup.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val axisDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

/**
 * Estimated one-rep max per session, over time.
 *
 * One series, so there is no legend — the title names it. Only the first, last and best
 * points are labelled rather than every point, and the record list below the chart is the
 * table view of the same data.
 */
@Composable
fun ProgressChart(
    points: List<ProgressPoint>,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
) {
    // Two points is the minimum that can show a direction; one is just a dot.
    if (points.size < 2) return

    val lineColor = ChartLine
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val values = points.map { it.estimated1rmKg }
    val best = values.max()
    val lowest = values.min()
    val first = values.first()
    val latest = values.last()

    // A flat series would divide by zero, so pad the range when everything is equal.
    val top = if (best == lowest) best + 1.0 else best
    val bottom = if (best == lowest) lowest - 1.0 else lowest
    val span = top - bottom

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Estimated 1RM",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${points.size} sessions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = WeightConverter.format(latest, unit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = changeLabel(first, latest, unit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                val markerRadius = 4.dp.toPx()
                val inset = markerRadius + 2.dp.toPx()
                val usableWidth = size.width - inset * 2
                val usableHeight = size.height - inset * 2

                // Three recessive gridlines: low, middle, high. No axis box.
                repeat(3) { index ->
                    val y = inset + usableHeight * index / 2f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val positions = points.mapIndexed { index, point ->
                    val x = inset + usableWidth * index / (points.size - 1).toFloat()
                    val ratio = ((point.estimated1rmKg - bottom) / span).toFloat()
                    val y = inset + usableHeight * (1f - ratio.coerceIn(0f, 1f))
                    Offset(x, y)
                }

                val path = Path().apply {
                    moveTo(positions.first().x, positions.first().y)
                    positions.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))

                // Endpoints and the best session get a marker; the rest stay unmarked so the
                // shape of the line is what reads.
                val bestIndex = values.indexOf(best)
                listOf(0, bestIndex, points.lastIndex).distinct().forEach { index ->
                    val centre = positions[index]
                    // A surface ring first, so a marker sitting on the line still reads as
                    // a separate mark rather than a bulge in it.
                    drawCircle(color = surfaceColor, radius = markerRadius + 2.dp.toPx(), center = centre)
                    drawCircle(color = lineColor, radius = markerRadius, center = centre)
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            val firstDate = points.first().date
            val lastDate = points.last().date
            val bestIsLatest = values.lastIndexOf(best) == points.lastIndex

            Row(
                modifier = Modifier.fillMaxWidth(),
                // A single day of sessions has nothing to span, so its one date is centred
                // rather than printed at both ends.
                horizontalArrangement = if (firstDate == lastDate) {
                    Arrangement.Center
                } else {
                    Arrangement.SpaceBetween
                },
            ) {
                Text(
                    text = firstDate.format(axisDateFormat),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (firstDate != lastDate) {
                    // The headline already gives the latest figure, so the middle label is
                    // only worth the space when the best session is not the latest one.
                    if (!bestIsLatest) {
                        Text(
                            text = "best ${WeightConverter.format(best, unit)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = lastDate.format(axisDateFormat),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun changeLabel(first: Double, latest: Double, unit: WeightUnit): String {
    val delta = latest - first
    val formatted = WeightConverter.format(abs(delta), unit)
    return when {
        delta > 0.01 -> "up $formatted since the start"
        delta < -0.01 -> "down $formatted since the start"
        else -> "no change since the start"
    }
}
