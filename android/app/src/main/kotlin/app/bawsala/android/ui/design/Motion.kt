package app.bawsala.android.ui.design

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Motion durations (#64): short and calm. */
object Motion {
    const val QUICK = 150
    const val STANDARD = 250
    const val SCREEN = 300
}

/**
 * True when the user turned animations off ("Remove animations" in Android's accessibility
 * settings): every V3 animation then snaps to its end state.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                ) == 0f
            }
            .getOrDefault(false)
    }
}

/** A tween of [durationMs], or an immediate change when motion is reduced. */
@Composable
fun <T> motionSpec(durationMs: Int = Motion.STANDARD): FiniteAnimationSpec<T> =
    if (rememberReduceMotion()) snap() else tween(durationMs, easing = FastOutSlowInEasing)
