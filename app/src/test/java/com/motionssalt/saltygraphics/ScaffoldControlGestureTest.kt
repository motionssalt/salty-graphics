package com.motionssalt.saltygraphics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.controls.SaltyProgressSlider
import com.motionssalt.saltygraphics.uikit.controls.SaltyRulerSlider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h860dp-mdpi")
class ScaffoldControlGestureTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun progressDragCommitsFinalValueOnceAndUsesReplacementCallbacks() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var value by mutableFloatStateOf(20f)
        var generation by mutableIntStateOf(0)
        val commits = mutableListOf<Pair<Int, Float>>()
        host.get().setContent { MaterialTheme { Box(Modifier.requiredSize(400.dp, 150.dp)) {
            val captured = generation
            SaltyProgressSlider(value, { value = it }, onValueCommit = { commits += captured to it }, range = 10f..110f, step = 10f)
        } } }
        try {
            compose.runOnIdle { generation = 1 }
            compose.onNodeWithTag("progress_track").performTouchInput {
                down(Offset(width * .2f, centerY)); moveTo(Offset(width * .8f, centerY), 200); up()
            }
            compose.runOnIdle {
                assertEquals(1, commits.size)
                assertEquals(1, commits.single().first)
                assertEquals(value, commits.single().second, .001f)
                assertEquals(90f, value, .001f)
            }
        } finally { host.pause().stop().destroy() }
    }

    @Test fun cancelledNumericGesturesRollBackPreviewWithoutCommit() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var progress by mutableFloatStateOf(20f)
        var ruler by mutableFloatStateOf(20f)
        var commits = 0
        var cancellations = 0
        host.get().setContent { MaterialTheme { Column(Modifier.requiredSize(400.dp, 260.dp)) {
            SaltyProgressSlider(progress, { progress = it }, onValueCommit = { commits++ },
                onValueCancel = { cancellations++; progress = 20f })
            SaltyRulerSlider(ruler, { ruler = it }, onValueCommit = { commits++ },
                onValueCancel = { cancellations++; ruler = 20f })
        } } }
        try {
            compose.onNodeWithTag("progress_track").performTouchInput {
                down(center); moveBy(Offset(50f, 0f), 200); cancel()
            }
            compose.onNodeWithTag("ruler_track").performTouchInput {
                down(center); moveBy(Offset(-50f, 0f), 200); cancel()
            }
            compose.runOnIdle {
                assertEquals(0, commits)
                assertEquals(2, cancellations)
                assertEquals(20f, progress, .001f); assertEquals(20f, ruler, .001f)
            }
        } finally { host.pause().stop().destroy() }
    }
}
