package com.wizlabs.wizdipcharts

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Whether continuous (looping) animation may run right now: the window has focus,
 * system animations are on and Battery Saver is off. One-shot transitions still run.
 */
@Composable
fun motionAllowed(): Boolean {
    val context = LocalContext.current
    val focused = LocalWindowInfo.current.isWindowFocused
    val power = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    // Re-read on every focus change, so toggling Battery Saver in quick settings is picked up.
    return focused && animatorsEnabled(context) && power?.isPowerSaveMode != true
}

private fun animatorsEnabled(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled()
    else Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

/** Fade + rise on first appearance, staggered by [index]. Runs in the draw/layer phase only. */
fun Modifier.enterStagger(index: Int): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(40L + index * 60L)
        a.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 22.dp.toPx()
    }
}

/** Click with a press-scale and springy release. */
fun Modifier.pressClick(scaleTo: Float = 0.95f, enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s = animateFloatAsState(
        if (pressed) scaleTo else 1f,
        spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    graphicsLayer { scaleX = s.value; scaleY = s.value }
        .clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
}
