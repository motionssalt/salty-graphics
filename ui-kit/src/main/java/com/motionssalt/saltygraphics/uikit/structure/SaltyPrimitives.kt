package com.motionssalt.saltygraphics.uikit.structure

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motionssalt.saltygraphics.uikit.controls.SaltyCheckerboard
import com.motionssalt.saltygraphics.uikit.tokens.SaltyTokens

enum class SaltySpace {
    NONE, XXS, XS, SM, MD, LG, XL, XXL, PANEL;

    @Composable
    fun dpValue(): Dp = when (this) {
        NONE -> SaltyTokens.spacing.none
        XXS -> SaltyTokens.spacing.xxs
        XS -> SaltyTokens.spacing.xs
        SM -> SaltyTokens.spacing.sm
        MD -> SaltyTokens.spacing.md
        LG -> SaltyTokens.spacing.lg
        XL -> SaltyTokens.spacing.xl
        XXL -> SaltyTokens.spacing.xxl
        PANEL -> SaltyTokens.spacing.panelPadding
    }
}

enum class SaltyTextStyle {
    TITLE_LARGE, TITLE_MEDIUM, TITLE_SMALL,
    BODY_LARGE, BODY_MEDIUM, BODY_SMALL,
    LABEL_LARGE, LABEL_MEDIUM, LABEL_SMALL,
    MONO
}

enum class SaltyTextTone {
    PRIMARY, SECONDARY, DISABLED, ACCENT, ON_PRIMARY, ERROR
}

enum class SaltyTextWeight {
    DEFAULT, NORMAL, MEDIUM, SEMI_BOLD, BOLD
}

enum class SaltyJustify {
    START, CENTER, END, SPACE_BETWEEN, SPACE_EVENLY, SPACE_AROUND
}

enum class SaltyIconSize {
    XS, SM, MD, LG, XL;

    @Composable
    fun dpValue(): Dp = when (this) {
        XS -> SaltyTokens.spacing.md
        SM -> SaltyTokens.spacing.lg
        MD -> SaltyTokens.spacing.toolbar20
        LG -> SaltyTokens.spacing.xl
        XL -> SaltyTokens.spacing.xxl
    }
}

class SaltyRowScope(private val scope: RowScope) {
    fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier = with(scope) { this@weight.weight(weight, fill) }
    fun Modifier.align(alignment: Alignment.Vertical): Modifier = with(scope) { this@align.align(alignment) }
}

class SaltyColumnScope(private val scope: ColumnScope) {
    fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier = with(scope) { this@weight.weight(weight, fill) }
    fun Modifier.align(alignment: Alignment.Horizontal): Modifier = with(scope) { this@align.align(alignment) }
}

class SaltyBoxScope(private val scope: BoxScope) {
    fun Modifier.align(alignment: Alignment): Modifier = with(scope) { this@align.align(alignment) }
}

fun Modifier.saltyFillMaxWidth(): Modifier = fillMaxWidth()
fun Modifier.saltyFillMaxHeight(): Modifier = fillMaxHeight()
fun Modifier.saltyFillMaxSize(): Modifier = fillMaxSize()
fun Modifier.saltyNavigationBarsPadding(): Modifier = navigationBarsPadding()
fun Modifier.saltyImePadding(): Modifier = imePadding()
fun Modifier.saltySafeDrawingPadding(): Modifier = safeDrawingPadding()
fun Modifier.saltyPadding(paddingValues: PaddingValues): Modifier = padding(paddingValues)
@Composable
fun Modifier.saltyHorizontalScroll(): Modifier = horizontalScroll(rememberScrollState())
fun Modifier.saltyClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = clickable(enabled = enabled, onClick = onClick)

@Composable
fun Modifier.saltyPadding(all: SaltySpace): Modifier = padding(all.dpValue())

@Composable
fun Modifier.saltyPadding(horizontal: SaltySpace = SaltySpace.NONE, vertical: SaltySpace = SaltySpace.NONE): Modifier =
    padding(horizontal = horizontal.dpValue(), vertical = vertical.dpValue())

@Composable
fun Modifier.saltySize(width: Dp? = null, height: Dp? = null): Modifier = when {
    width != null && height != null -> size(width, height)
    width != null -> width(width)
    height != null -> height(height)
    else -> this
}

@Composable
private fun resolveSpace(space: SaltySpace): Dp = space.dpValue()

@Composable
private fun toHorizontalArrangement(justify: SaltyJustify, spacing: SaltySpace): Arrangement.Horizontal {
    val dp = resolveSpace(spacing)
    return when {
        justify == SaltyJustify.SPACE_BETWEEN -> Arrangement.SpaceBetween
        justify == SaltyJustify.SPACE_EVENLY -> Arrangement.SpaceEvenly
        justify == SaltyJustify.SPACE_AROUND -> Arrangement.SpaceAround
        spacing != SaltySpace.NONE -> Arrangement.spacedBy(dp)
        justify == SaltyJustify.CENTER -> Arrangement.Center
        justify == SaltyJustify.END -> Arrangement.End
        else -> Arrangement.Start
    }
}

@Composable
private fun toVerticalArrangement(justify: SaltyJustify, spacing: SaltySpace): Arrangement.Vertical {
    val dp = resolveSpace(spacing)
    return when {
        justify == SaltyJustify.SPACE_BETWEEN -> Arrangement.SpaceBetween
        justify == SaltyJustify.SPACE_EVENLY -> Arrangement.SpaceEvenly
        justify == SaltyJustify.SPACE_AROUND -> Arrangement.SpaceAround
        spacing != SaltySpace.NONE -> Arrangement.spacedBy(dp)
        justify == SaltyJustify.CENTER -> Arrangement.Center
        justify == SaltyJustify.END -> Arrangement.Bottom
        else -> Arrangement.Top
    }
}

@Composable
fun SaltyText(
    text: String,
    modifier: Modifier = Modifier,
    style: SaltyTextStyle = SaltyTextStyle.BODY_MEDIUM,
    tone: SaltyTextTone = SaltyTextTone.PRIMARY,
    weight: SaltyTextWeight = SaltyTextWeight.DEFAULT,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
    textAlign: TextAlign? = null,
    fontFamily: FontFamily? = null
) {
    val colors = SaltyTokens.colors
    val baseStyle = when (style) {
        SaltyTextStyle.TITLE_LARGE -> SaltyTokens.typography.titleLarge
        SaltyTextStyle.TITLE_MEDIUM -> SaltyTokens.typography.titleMedium
        SaltyTextStyle.TITLE_SMALL -> SaltyTokens.typography.titleSmall
        SaltyTextStyle.BODY_LARGE -> SaltyTokens.typography.bodyLarge
        SaltyTextStyle.BODY_MEDIUM -> SaltyTokens.typography.bodyMedium
        SaltyTextStyle.BODY_SMALL -> SaltyTokens.typography.bodySmall
        SaltyTextStyle.LABEL_LARGE -> SaltyTokens.typography.labelLarge
        SaltyTextStyle.LABEL_MEDIUM -> SaltyTokens.typography.labelMedium
        SaltyTextStyle.LABEL_SMALL -> SaltyTokens.typography.labelSmall
        SaltyTextStyle.MONO -> SaltyTokens.typography.mono
    }
    val resolvedWeight = when (weight) {
        SaltyTextWeight.DEFAULT -> baseStyle.fontWeight
        SaltyTextWeight.NORMAL -> FontWeight.Normal
        SaltyTextWeight.MEDIUM -> FontWeight.Medium
        SaltyTextWeight.SEMI_BOLD -> FontWeight.SemiBold
        SaltyTextWeight.BOLD -> FontWeight.Bold
    }
    val resolvedColor = when (tone) {
        SaltyTextTone.PRIMARY -> colors.textPrimary
        SaltyTextTone.SECONDARY -> colors.textSecondary
        SaltyTextTone.DISABLED -> colors.textDisabled
        SaltyTextTone.ACCENT -> colors.primary
        SaltyTextTone.ON_PRIMARY -> colors.onPrimary
        SaltyTextTone.ERROR -> colors.error
    }
    Text(
        text = text,
        modifier = modifier,
        style = baseStyle.copy(
            fontWeight = resolvedWeight,
            fontFamily = fontFamily ?: baseStyle.fontFamily
        ),
        color = resolvedColor,
        maxLines = maxLines,
        overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
        textAlign = textAlign
    )
}

@Composable
fun SaltyIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tone: SaltyTextTone = SaltyTextTone.PRIMARY,
    size: SaltyIconSize = SaltyIconSize.MD
) {
    val colors = SaltyTokens.colors
    val tint = when (tone) {
        SaltyTextTone.PRIMARY -> colors.textPrimary
        SaltyTextTone.SECONDARY -> colors.textSecondary
        SaltyTextTone.DISABLED -> colors.textDisabled
        SaltyTextTone.ACCENT -> colors.primary
        SaltyTextTone.ON_PRIMARY -> colors.onPrimary
        SaltyTextTone.ERROR -> colors.error
    }
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size.dpValue())
    )
}

@Composable
fun SaltyColumn(
    modifier: Modifier = Modifier,
    spacing: SaltySpace = SaltySpace.NONE,
    padding: SaltySpace = SaltySpace.NONE,
    justify: SaltyJustify = SaltyJustify.START,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scrollable: Boolean = false,
    content: @Composable SaltyColumnScope.() -> Unit
) {
    val pad = resolveSpace(padding)
    val base = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier
    Column(
        modifier = base.padding(pad),
        verticalArrangement = toVerticalArrangement(justify, spacing),
        horizontalAlignment = horizontalAlignment
    ) {
        SaltyColumnScope(this).content()
    }
}

@Composable
fun SaltyRow(
    modifier: Modifier = Modifier,
    spacing: SaltySpace = SaltySpace.NONE,
    padding: SaltySpace = SaltySpace.NONE,
    justify: SaltyJustify = SaltyJustify.START,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    scrollable: Boolean = false,
    content: @Composable SaltyRowScope.() -> Unit
) {
    val pad = resolveSpace(padding)
    val base = if (scrollable) modifier.horizontalScroll(rememberScrollState()) else modifier
    Row(
        modifier = base.padding(pad),
        horizontalArrangement = toHorizontalArrangement(justify, spacing),
        verticalAlignment = verticalAlignment
    ) {
        SaltyRowScope(this).content()
    }
}

@Composable
fun SaltyBox(
    modifier: Modifier = Modifier,
    padding: SaltySpace = SaltySpace.NONE,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable SaltyBoxScope.() -> Unit
) {
    Box(
        modifier = modifier.padding(resolveSpace(padding)),
        contentAlignment = contentAlignment
    ) {
        SaltyBoxScope(this).content()
    }
}

@Composable
fun SaltyLazyColumn(
    modifier: Modifier = Modifier,
    spacing: SaltySpace = SaltySpace.SM,
    contentPadding: SaltySpace = SaltySpace.NONE,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(resolveSpace(spacing)),
        contentPadding = PaddingValues(resolveSpace(contentPadding)),
        content = content
    )
}

@Composable
fun SaltyAdaptiveGrid(
    minCellSize: Dp = 160.dp,
    modifier: Modifier = Modifier,
    spacing: SaltySpace = SaltySpace.MD,
    contentPadding: PaddingValues = PaddingValues(resolveSpace(SaltySpace.LG)),
    content: LazyGridScope.() -> Unit
) {
    val dp = resolveSpace(spacing)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCellSize),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(dp),
        verticalArrangement = Arrangement.spacedBy(dp),
        modifier = modifier,
        content = content
    )
}

@Composable
fun SaltySpacer(space: SaltySpace = SaltySpace.SM, horizontal: Boolean = false) {
    val dp = resolveSpace(space)
    if (horizontal) Spacer(Modifier.width(dp)) else Spacer(Modifier.height(dp))
}

@Composable
fun SaltyDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = SaltyTokens.colors.outlineVariant
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SaltyCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    padding: SaltySpace = SaltySpace.PANEL,
    spacing: SaltySpace = SaltySpace.SM,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable SaltyColumnScope.() -> Unit
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    val interactionModifier = when {
        onClick != null && onLongClick != null -> modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        onClick != null -> modifier.clickable(onClick = onClick)
        else -> modifier
    }
    Card(
        modifier = interactionModifier.fillMaxWidth(),
        shape = shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.surfaceVariant else colors.surface
        ),
        border = if (isSelected) BorderStroke(1.5.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(resolveSpace(padding)),
            verticalArrangement = Arrangement.spacedBy(resolveSpace(spacing))
        ) {
            SaltyColumnScope(this).content()
        }
    }
}

@Composable
fun SaltySectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable SaltyColumnScope.() -> Unit
) {
    SaltyCard(modifier = modifier, padding = SaltySpace.PANEL, spacing = SaltySpace.MD) {
        if (title != null) {
            SaltyText(
                text = title,
                style = SaltyTextStyle.TITLE_MEDIUM,
                tone = SaltyTextTone.ACCENT,
                weight = SaltyTextWeight.SEMI_BOLD
            )
        }
        content()
    }
}

@Composable
fun SaltyWarningCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.card,
        colors = CardDefaults.cardColors(containerColor = colors.secondary.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, colors.secondary)
    ) {
        Row(
            modifier = Modifier.padding(SaltyTokens.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.secondary,
                modifier = Modifier.size(SaltyTokens.spacing.xl)
            )
            Column(verticalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.xs)) {
                SaltyText(title, style = SaltyTextStyle.TITLE_SMALL, tone = SaltyTextTone.ACCENT, weight = SaltyTextWeight.BOLD)
                SaltyText(message, style = SaltyTextStyle.BODY_SMALL, tone = SaltyTextTone.PRIMARY)
            }
        }
    }
}

@Composable
fun SaltyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fillWidth: Boolean = false
) {
    val colors = SaltyTokens.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = SaltyTokens.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary
        ),
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(SaltyTokens.spacing.toolbar20))
            Spacer(Modifier.width(SaltyTokens.spacing.sm))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SaltyTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fillWidth: Boolean = false
) {
    val colors = SaltyTokens.colors
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = SaltyTokens.shapes.button,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.textPrimary
        ),
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(SaltyTokens.spacing.lg))
            Spacer(Modifier.width(SaltyTokens.spacing.xs))
        }
        Text(text)
    }
}

@Composable
fun SaltyOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fillWidth: Boolean = false
) {
    val colors = SaltyTokens.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = SaltyTokens.shapes.button,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
        border = BorderStroke(1.dp, colors.outline),
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(SaltyTokens.spacing.lg))
            Spacer(Modifier.width(SaltyTokens.spacing.xs))
        }
        Text(text)
    }
}

@Composable
fun SaltyTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tone: SaltyTextTone = SaltyTextTone.ACCENT
) {
    val colors = SaltyTokens.colors
    val tint = when (tone) {
        SaltyTextTone.ACCENT -> colors.primary
        SaltyTextTone.SECONDARY -> colors.textSecondary
        SaltyTextTone.ERROR -> colors.error
        else -> colors.textPrimary
    }
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(text, color = tint)
        if (icon != null) {
            Spacer(Modifier.width(SaltyTokens.spacing.xs))
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(SaltyTokens.spacing.lg))
        }
    }
}

@Composable
fun SaltyIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: SaltyTextTone = SaltyTextTone.PRIMARY,
    compact: Boolean = false
) {
    val colors = SaltyTokens.colors
    val tint = when {
        !enabled -> colors.textDisabled
        tone == SaltyTextTone.ACCENT -> colors.primary
        tone == SaltyTextTone.SECONDARY -> colors.textSecondary
        tone == SaltyTextTone.ERROR -> colors.error
        else -> colors.textPrimary
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = if (compact) modifier.size(SaltyTokens.spacing.toolbar36) else modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = if (compact) Modifier.size(SaltyTokens.spacing.toolbar20) else Modifier
        )
    }
}

@Composable
fun SaltyToggleIconButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedIcon: ImageVector,
    uncheckedIcon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    FilledIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = colors.surfaceVariant,
            checkedContainerColor = colors.primary,
            contentColor = colors.textSecondary,
            checkedContentColor = colors.onPrimary
        )
    ) {
        Icon(if (checked) checkedIcon else uncheckedIcon, contentDescription = contentDescription)
    }
}

@Composable
fun SaltyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label, style = SaltyTokens.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.primary.copy(alpha = 0.2f),
            selectedLabelColor = colors.primary
        )
    )
}

@Composable
fun SaltyTabRow(
    tabs: List<String>,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIdx = tabs.indexOf(selectedTab).coerceAtLeast(0)
    ScrollableTabRow(
        selectedTabIndex = selectedIdx,
        edgePadding = SaltyTokens.spacing.none,
        modifier = modifier,
        containerColor = SaltyTokens.colors.surface,
        contentColor = SaltyTokens.colors.primary
    ) {
        tabs.forEach { title ->
            Tab(
                selected = title == selectedTab,
                onClick = { onSelectTab(title) },
                text = { Text(title, style = SaltyTokens.typography.labelMedium) }
            )
        }
    }
}

@Composable
fun SaltySelectableTile(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null
) {
    val colors = SaltyTokens.colors
    val shapes = SaltyTokens.shapes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.tile)
            .background(if (selected) colors.surfaceVariant else colors.background)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.primary else colors.outline,
                shape = shapes.tile
            )
            .clickable(onClick = onClick)
            .padding(horizontal = SaltyTokens.spacing.md, vertical = SaltyTokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SaltyText(
                text = title,
                style = SaltyTextStyle.BODY_LARGE,
                tone = if (selected) SaltyTextTone.ACCENT else SaltyTextTone.PRIMARY,
                weight = if (selected) SaltyTextWeight.BOLD else SaltyTextWeight.NORMAL
            )
            if (subtitle != null) {
                SaltyText(subtitle, style = SaltyTextStyle.LABEL_SMALL, tone = SaltyTextTone.SECONDARY)
            }
        }
        if (badge != null) {
            SaltyBadge(text = badge)
        }
    }
}

@Composable
fun SaltyBadge(text: String, modifier: Modifier = Modifier, tone: SaltyTextTone = SaltyTextTone.ACCENT) {
    val colors = SaltyTokens.colors
    val bg = when (tone) {
        SaltyTextTone.ERROR -> colors.error.copy(alpha = 0.2f)
        SaltyTextTone.SECONDARY -> colors.secondary.copy(alpha = 0.2f)
        else -> colors.primary.copy(alpha = 0.2f)
    }
    Surface(
        color = bg,
        shape = SaltyTokens.shapes.xs,
        modifier = modifier
    ) {
        SaltyText(
            text = text,
            style = SaltyTextStyle.LABEL_SMALL,
            tone = tone,
            modifier = Modifier.padding(horizontal = SaltyTokens.spacing.xs, vertical = SaltyTokens.spacing.xxs)
        )
    }
}

@Composable
fun SaltyColorSwatch(
    colorArgb: Long,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    circular: Boolean = false
) {
    val colors = SaltyTokens.colors
    val shape = if (circular) CircleShape else SaltyTokens.shapes.sm
    val isTransparent = colorArgb == 0L || (colorArgb and 0xFF000000L) == 0L
    Box(
        modifier = modifier
            .size(SaltyTokens.spacing.toolbar36)
            .clip(shape)
            .border(1.5.dp, colors.outline, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        SaltyCheckerboard(Modifier.fillMaxSize(), squareSizePx = 8f)
        if (!isTransparent) {
            Box(Modifier.fillMaxSize().background(Color(colorArgb.toInt())))
        }
    }
}

@Composable
fun SaltyColorButton(
    colorArgb: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SaltyTokens.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = SaltyTokens.shapes.button,
        border = BorderStroke(1.dp, colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        )
    ) {
        Box(
            modifier = Modifier
                .size(SaltyTokens.spacing.lg)
                .clip(CircleShape)
                .background(Color(colorArgb.toInt()))
                .border(1.dp, colors.outline, CircleShape)
        )
        Spacer(Modifier.width(SaltyTokens.spacing.sm))
        SaltyText(
            text = String.format(java.util.Locale.US, "#%08X", colorArgb),
            style = SaltyTextStyle.MONO,
            tone = SaltyTextTone.PRIMARY
        )
    }
}

@Composable
fun SaltyProgressBar(progress: Float? = null, modifier: Modifier = Modifier) {
    val colors = SaltyTokens.colors
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier.fillMaxWidth(),
            color = colors.primary,
            trackColor = colors.surfaceVariant
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth(),
            color = colors.primary,
            trackColor = colors.surfaceVariant
        )
    }
}

@Composable
fun SaltySpinner(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        color = SaltyTokens.colors.primary
    )
}

@Composable
fun SaltyBitmapImage(
    bitmap: Bitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fitCenter: Boolean = true
) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = if (fitCenter) ContentScale.Fit else ContentScale.Crop
    )
}

@Composable
fun SaltyInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SaltyTokens.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SaltyText(label, style = SaltyTextStyle.BODY_MEDIUM, tone = SaltyTextTone.SECONDARY)
        SaltyText(value, style = SaltyTextStyle.BODY_MEDIUM, tone = SaltyTextTone.PRIMARY, weight = SaltyTextWeight.MEDIUM)
    }
}

@Composable
fun SaltyActionRow(
    label: String,
    detail: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SaltyTokens.shapes.md)
            .clickable(onClick = onClick)
            .padding(vertical = SaltyTokens.spacing.sm, horizontal = SaltyTokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SaltyTokens.spacing.md)
        ) {
            SaltyIcon(icon, tone = SaltyTextTone.SECONDARY, size = SaltyIconSize.MD)
            SaltyText(label, style = SaltyTextStyle.BODY_MEDIUM, tone = SaltyTextTone.PRIMARY)
        }
        SaltyText(detail, style = SaltyTextStyle.BODY_MEDIUM, tone = SaltyTextTone.ACCENT)
    }
}

data class SaltyTopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val tone: SaltyTextTone = SaltyTextTone.PRIMARY,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaltyScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    accentTitle: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: List<SaltyTopBarAction> = emptyList(),
    fabIcon: ImageVector? = null,
    fabContentDescription: String = "",
    onFabClick: (() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val colors = SaltyTokens.colors
    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    SaltyText(
                        text = title,
                        style = SaltyTextStyle.TITLE_LARGE,
                        tone = if (accentTitle) SaltyTextTone.ACCENT else SaltyTextTone.PRIMARY,
                        weight = SaltyTextWeight.BOLD
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                        }
                    }
                },
                actions = {
                    actions.forEach { action ->
                        SaltyIconButton(
                            icon = action.icon,
                            contentDescription = action.contentDescription,
                            onClick = action.onClick,
                            tone = action.tone
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary,
                    navigationIconContentColor = colors.textPrimary
                )
            )
        },
        floatingActionButton = {
            if (fabIcon != null && onFabClick != null) {
                FloatingActionButton(
                    onClick = onFabClick,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Icon(fabIcon, contentDescription = fabContentDescription)
                }
            }
        },
        bottomBar = {
            if (bottomBar != null) {
                Surface(
                    color = colors.surface,
                    tonalElevation = SaltyTokens.elevation.lg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    bottomBar()
                }
            }
        },
        content = { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                content(innerPadding)
                SaltyToastHost(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(innerPadding)
                        .padding(SaltyTokens.spacing.md)
                )
            }
        }
    )
}


@Composable
fun SaltyRootSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = SaltyTokens.colors.background,
        content = content
    )
}

@Composable
fun SaltyEditorScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        containerColor = SaltyTokens.colors.background
    ) { innerPadding ->
        content(
            Modifier
                .padding(innerPadding)
                .navigationBarsPadding()
                .displayCutoutPadding()
        )
    }
}

@Composable
fun SaltyAnimatedSection(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(visible = visible) {
        content()
    }
}


fun <T> LazyGridScope.saltyGridItems(
    items: List<T>,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    if (key != null) {
        gridItems(items, key = { key(it) }) { item -> itemContent(item) }
    } else {
        gridItems(items) { item -> itemContent(item) }
    }
}

fun <T> LazyListScope.saltyListItems(
    items: List<T>,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    if (key != null) {
        items(items, key = { key(it) }) { item -> itemContent(item) }
    } else {
        items(items) { item -> itemContent(item) }
    }
}


typealias SaltyPanelColumnScope = ColumnScope

@Composable
fun saltyIsSystemInDarkTheme(): Boolean = isSystemInDarkTheme()

@Composable
fun SaltyCanvasSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}
