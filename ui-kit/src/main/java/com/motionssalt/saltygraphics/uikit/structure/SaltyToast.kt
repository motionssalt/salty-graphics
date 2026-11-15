package com.motionssalt.saltygraphics.uikit.structure

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Contextual non-blocking toast component per V19 requirements.
 */
@Composable
fun SaltyToast(
    message: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(shapes.pill)
                .background(colors.surfaceVariant.copy(alpha = 0.95f))
                .border(1.dp, colors.outline, shapes.pill)
                .padding(horizontal = SaltyTokens.spacing.lg, vertical = SaltyTokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = message,
                style = SaltyTokens.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}



data class SaltyToastMessage(
    val id: Long,
    val text: String,
    val durationMs: Long = 2200L,
    val icon: ImageVector? = null
)

/**
 * In-app toast queue manager (V34): max 2 visible at once, auto-dismissing,
 * never covering bottom-panel controls and never blocking canvas touches.
 */
object SaltyToastManager {
    private val nextId = AtomicLong(1L)
    private val _queue = MutableStateFlow<List<SaltyToastMessage>>(emptyList())
    val queue: StateFlow<List<SaltyToastMessage>> = _queue.asStateFlow()

    fun show(message: String, durationMs: Long = 2200L, icon: ImageVector? = null) {
        if (message.isBlank()) return
        val item = SaltyToastMessage(
            id = nextId.getAndIncrement(),
            text = message,
            durationMs = durationMs.coerceAtLeast(300L),
            icon = icon
        )
        _queue.value = _queue.value + item
    }

    fun dismiss(id: Long) {
        _queue.value = _queue.value.filterNot { it.id == id }
    }

    fun clear() {
        _queue.value = emptyList()
    }
}

@Composable
fun SaltyToastHost(
    modifier: Modifier = Modifier,
    manager: SaltyToastManager = SaltyToastManager
) {
    val queue by manager.queue.collectAsState()
    val visible = queue.take(2)
    visible.forEach { msg ->
        LaunchedEffect(msg.id) {
            delay(msg.durationMs)
            manager.dismiss(msg.id)
        }
    }
    Column(
        modifier = modifier.testTag("salty_toast_host"),
        verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs),
        horizontalAlignment = Alignment.Start
    ) {
        visible.forEach { msg ->
            key(msg.id) {
                SaltyToast(
                    message = msg.text,
                    visible = true,
                    icon = msg.icon,
                    modifier = Modifier.testTag("salty_toast_item")
                )
            }
        }
    }
}

/**
 * Standard confirmation dialog for destructive or irreversible actions.
 * Allowed exception under the standing UI rule (rasterize, delete project).
 */
@Composable
fun SaltyConfirmDialog(
    title: String,
    message: String = "",
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    secondaryConfirmText: String? = null,
    onSecondaryConfirm: (() -> Unit)? = null,
    bodyContent: (@Composable () -> Unit)? = null
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapes.dialog,
        containerColor = colors.surface,
        title = {
            Text(
                text = title,
                style = SaltyTokens.typography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (bodyContent != null) {
                bodyContent()
            } else {
                Text(
                    text = message,
                    style = SaltyTokens.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sm)) {
                TextButton(onClick = onDismiss) {
                    Text(dismissText, color = colors.textSecondary)
                }
                if (secondaryConfirmText != null && onSecondaryConfirm != null) {
                    Button(
                        onClick = onSecondaryConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.secondary,
                            contentColor = colors.onSecondary
                        )
                    ) {
                        Text(secondaryConfirmText)
                    }
                }
            }
        }
    )
}
