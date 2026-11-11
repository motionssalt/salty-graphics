package com.motionssalt.saltygraphics.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionssalt.saltygraphics.engine.model.ShapeData
import com.motionssalt.saltygraphics.engine.model.ShapeType

data class ShapePickerItem(
    val type: ShapeType,
    val name: String,
    val icon: ImageVector,
    val defaultShape: ShapeData
)

/**
 * Shape picker for core shapes (T18):
 * Rectangle (and rounded rectangle), Ellipse (and circle), Line, Arrow, Polygon, and Star.
 */
@Composable
fun ShapePickerDialog(
    onSelectShape: (ShapeData) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val shapes = listOf(
        ShapePickerItem(
            type = ShapeType.ROUNDED_RECTANGLE,
            name = "Rounded Rect",
            icon = Icons.Default.RoundedCorner,
            defaultShape = ShapeData(type = ShapeType.ROUNDED_RECTANGLE, width = 300f, height = 200f, cornerRadius = 32f)
        ),
        ShapePickerItem(
            type = ShapeType.RECTANGLE,
            name = "Rectangle",
            icon = Icons.Default.CropSquare,
            defaultShape = ShapeData(type = ShapeType.RECTANGLE, width = 300f, height = 200f)
        ),
        ShapePickerItem(
            type = ShapeType.CIRCLE,
            name = "Circle / Ellipse",
            icon = Icons.Default.Circle,
            defaultShape = ShapeData(type = ShapeType.CIRCLE, width = 250f, height = 250f)
        ),
        ShapePickerItem(
            type = ShapeType.STAR,
            name = "Star",
            icon = Icons.Default.Star,
            defaultShape = ShapeData(type = ShapeType.STAR, width = 280f, height = 280f, starPoints = 5, starInnerRadiusRatio = 0.5f)
        ),
        ShapePickerItem(
            type = ShapeType.POLYGON,
            name = "Polygon",
            icon = Icons.Default.Hexagon,
            defaultShape = ShapeData(type = ShapeType.POLYGON, width = 280f, height = 280f, sides = 6)
        ),
        ShapePickerItem(
            type = ShapeType.LINE,
            name = "Line",
            icon = Icons.Default.HorizontalRule,
            defaultShape = ShapeData(type = ShapeType.LINE, width = 300f, height = 4f, strokeWidth = 6f, hasStroke = true, hasFill = false)
        ),
        ShapePickerItem(
            type = ShapeType.ARROW,
            name = "Arrow",
            icon = Icons.Default.ArrowForward,
            defaultShape = ShapeData(type = ShapeType.ARROW, width = 300f, height = 32f, strokeWidth = 6f, hasStroke = true, hasFill = false)
        )
    )

    PanelPage(title = "Choose a Shape", onBack = onDismiss, scrollable = true) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shapes) { item ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .clickable {
                            onSelectShape(item.defaultShape)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(item.icon, contentDescription = item.name, tint = colors.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(item.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}
