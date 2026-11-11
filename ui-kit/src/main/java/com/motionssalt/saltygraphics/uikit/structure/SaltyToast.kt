package com.motionssalt.saltygraphics.uikit.structure

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

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

/**
 * Standard confirmation dialog for destructive or irreversible actions.
 * Allowed exception under the standing UI rule (rasterize, delete project).
 */
@Composable
fun SaltyConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    secondaryConfirmText: String? = null,
    onSecondaryConfirm: (() -> Unit)? = null
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
            Text(
                text = message,
                style = SaltyTokens.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
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
