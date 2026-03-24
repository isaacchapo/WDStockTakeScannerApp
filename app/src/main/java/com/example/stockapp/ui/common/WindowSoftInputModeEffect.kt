package com.example.stockapp.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Suppress("DEPRECATION")
const val SoftInputAdjustResizeMode: Int = android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

@Suppress("DEPRECATION")
const val SoftInputAdjustPanMode: Int = android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN

@Suppress("DEPRECATION")
const val SoftInputAdjustNothingMode: Int = android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING

@Composable
fun WindowSoftInputModeEffect(adjustMode: Int) {
    val context = LocalContext.current

    DisposableEffect(context, adjustMode) {
        val activity = context.findActivity()
        val window = activity?.window
        val previousSoftInputMode = window?.attributes?.softInputMode

        if (window != null) {
            val preservedState = (previousSoftInputMode ?: 0) and android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE
            window.setSoftInputMode(preservedState or adjustMode)
        }

        onDispose {
            if (window != null && previousSoftInputMode != null) {
                window.setSoftInputMode(previousSoftInputMode)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
