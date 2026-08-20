package by.vsdev.cpt.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
actual fun rememberFoldPosture(): CptFoldPosture {
    val activity = LocalContext.current.findActivity() ?: return CptFoldPosture.None
    val windowInfoTracker = remember(activity) { WindowInfoTracker.getOrCreate(activity) }
    val windowLayoutInfo by
        produceState<WindowLayoutInfo?>(initialValue = null, windowInfoTracker, activity) {
            windowInfoTracker.windowLayoutInfo(activity).collect { value = it }
        }
    val displayFeatures = windowLayoutInfo?.displayFeatures ?: return CptFoldPosture.None
    val foldingFeature =
        displayFeatures
            .filterIsInstance<FoldingFeature>()
            .firstOrNull { it.state == FoldingFeature.State.HALF_OPENED }
    if (foldingFeature == null) return CptFoldPosture.None

    val containerSize = LocalWindowInfo.current.containerSize
    val isVertical = foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL
    val bounds = foldingFeature.bounds
    return if (isVertical && containerSize.width > 0) {
        CptFoldPosture.HalfOpened(
            isHingeVertical = true,
            hingeStartFraction = bounds.left.toFloat() / containerSize.width,
            hingeEndFraction = bounds.right.toFloat() / containerSize.width,
        )
    } else if (!isVertical && containerSize.height > 0) {
        CptFoldPosture.HalfOpened(
            isHingeVertical = false,
            hingeStartFraction = bounds.top.toFloat() / containerSize.height,
            hingeEndFraction = bounds.bottom.toFloat() / containerSize.height,
        )
    } else {
        CptFoldPosture.None
    }
}
