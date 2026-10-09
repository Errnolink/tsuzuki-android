package dev.errnolink.tsuzuki.designsystem

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.HazeInputScale

internal val LocalInsideGlass = staticCompositionLocalOf { false }
val LocalNavigationMotion = compositionLocalOf { false }

@Stable
class GlassContext internal constructor(
    internal val haze: HazeState,
    internal val backdrop: LayerBackdrop?,
    internal val enabled: Boolean,
) {
    var navigationMinimized by mutableStateOf(false)
        internal set
    private var scrollDistance = 0f
    internal val navigationScroll = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) {
                val delta = consumed.y
                if (delta * scrollDistance < 0f) scrollDistance = 0f
                scrollDistance += delta
                if (scrollDistance < -48f) navigationMinimized = true
                if (scrollDistance > 24f || available.y > 0f) navigationMinimized = false
            }
            return Offset.Zero
        }
    }
}

fun Modifier.floatingNavigationScroll(context: GlassContext): Modifier = nestedScroll(context.navigationScroll)

@Composable
fun rememberGlassContext(enableHeroLens: Boolean = false): GlassContext {
    val enabled = !LocalOpaqueGlass.current && Build.VERSION.SDK_INT >= 31
    val haze = rememberHazeState(blurEnabled = enabled)
    val backdrop = if (enabled && enableHeroLens && Build.VERSION.SDK_INT >= 33) rememberLayerBackdrop() else null
    return remember(haze, backdrop, enabled) { GlassContext(haze, backdrop, enabled) }
}

fun Modifier.glassSource(context: GlassContext): Modifier = if (context.enabled) {
    hazeSource(context.haze).then(context.backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
} else {
    this
}

@Composable
fun GlassSurface(
    context: GlassContext?,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(TsuzukiCorners.capsulePercent),
    hero: Boolean = false,
    material: GlassMaterial = GlassMaterial.Regular,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = TsuzukiTheme.colors
    val nested = LocalInsideGlass.current
    val opaque = LocalOpaqueGlass.current || material == GlassMaterial.Opaque
    val tintStrength = LocalGlassTintStrength.current
    val enabled = context?.enabled == true && context.haze.areas.isNotEmpty() &&
        !opaque && !nested && !LocalNavigationMotion.current
    val tint = colors.glassTint.copy(alpha = material.tintAlpha(colors.isDark, tintStrength))
    val fallback = colors.elevated.copy(alpha = if (opaque) 1f else material.fallbackAlpha)
    val blur = material.blurRadius
    val style = remember(colors, tint, fallback, blur) {
        HazeStyle(
            backgroundColor = colors.background,
            tint = HazeTint(tint),
            blurRadius = blur,
            noiseFactor = GlassMaterial.NoiseFactor,
            fallbackTint = HazeTint(fallback),
        )
    }
    val surface = when {
        !enabled -> Modifier.background(
            if (nested) Color.Transparent else fallback,
            shape,
        )
        hero && Build.VERSION.SDK_INT >= 33 && context.backdrop != null -> Modifier.drawBackdrop(
            backdrop = context.backdrop,
            shape = { shape },
            effects = {
                blur(blur.toPx())
                lens(8.dp.toPx(), 12.dp.toPx())
            },
            onDrawSurface = { drawRect(tint) },
        )
        else -> Modifier.hazeEffect(state = context.haze, style = style) {
            inputScale = HazeInputScale.Fixed(GlassMaterial.InputScale)
        }
    }
    val rim = if (nested) Modifier else Modifier.drawWithCache {
        val ring = shape.createOutline(size, layoutDirection, this)
        val inset = GlassMaterial.HighlightInset.toPx()
        val inner = shape.createOutline(
            Size((size.width - 2 * inset).coerceAtLeast(0f), (size.height - 2 * inset).coerceAtLeast(0f)),
            layoutDirection,
            this,
        )
        val ringColor = if (colors.isDark) GlassMaterial.DarkRing else GlassMaterial.LightRing
        val highlight = Brush.verticalGradient(
            listOf(if (colors.isDark) GlassMaterial.DarkHighlight else GlassMaterial.LightHighlight, Color.Transparent),
        )
        val ringStroke = Stroke(GlassMaterial.RingWidth.toPx())
        val highlightStroke = Stroke(GlassMaterial.HighlightWidth.toPx())
        onDrawWithContent {
            drawContent()
            drawOutline(ring, ringColor, style = ringStroke)
            translate(inset, inset) { drawOutline(inner, highlight, style = highlightStroke) }
        }
    }
    Box(modifier.clip(shape).then(surface).then(rim)) {
        CompositionLocalProvider(LocalInsideGlass provides true) { content() }
    }
}
