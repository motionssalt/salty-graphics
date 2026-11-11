package com.motionssalt.saltygraphics.uikit.tokens
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
/** Shared dimensions and motion. Registered feature data contains no design values. */
object SaltySpacing {
    val none = 0.dp; val hairline = 1.dp; val xs = 2.dp; val sm = 4.dp
    val md = 8.dp; val lg = 12.dp; val xl = 16.dp; val xxl = 24.dp
    val touchTarget = 48.dp; val panelMinHeight = 260.dp; val panelMaxHeight = 340.dp
}
object SaltyShapes {
    val small = RoundedCornerShape(6.dp)
    val medium = RoundedCornerShape(10.dp)
    val large = RoundedCornerShape(16.dp)
}
object SaltyElevation { val none = 0.dp; val toolbar = 6.dp; val panel = 8.dp }
object SaltyMotion { const val fastMillis = 120; const val standardMillis = 200 }
