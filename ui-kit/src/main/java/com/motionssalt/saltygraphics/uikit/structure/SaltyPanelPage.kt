package com.motionssalt.saltygraphics.uikit.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

/**
 * Standard bottom panel page structure.
 * Features a header with back navigation / close action, title, and vertically scrolling body.
 * Adheres strictly to the standing UI rule: editing lives inside the bottom panel.
 */
@Composable
fun SaltyPanelPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SaltyTokens.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(colors.surface)
    ) {
        // Panel Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(colors.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = SaltyTokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = title,
                    style = SaltyTokens.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (action != null) {
                action()
            }
        }

        // Panel Body
        val bodyModifier = if (scrollable) {
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(SaltyTokens.spacing.panelPadding)
        } else {
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(SaltyTokens.spacing.panelPadding)
        }

        Column(
            modifier = bodyModifier,
            verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.sectionSpacing),
            content = content
        )
    }
}
