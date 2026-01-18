package com.example.adshield.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * UI Access States for Free/Trial/Premium feature gating
 */
enum class UiAccessState {
    /** Full access - no visual changes */
    FULL,
    /** Dimmed - visible but indicates limited access */
    DIM,
    /** Locked - tapping shows paywall */
    LOCK
}

// ============================================================
// MODIFIER EXTENSIONS
// ============================================================

/**
 * Dims the content with reduced alpha.
 * Use for features that are visible but limited for FREE users.
 */
fun Modifier.dim(alpha: Float = 0.4f): Modifier = this.graphicsLayer { 
    this.alpha = alpha 
}

/**
 * Locks the content - dims it and intercepts all taps.
 * Use for premium-only features.
 */
fun Modifier.lock(
    alpha: Float = 0.35f,
    onClickLocked: (() -> Unit)? = null
): Modifier = this
    .graphicsLayer { this.alpha = alpha }
    .pointerInput(Unit) {
        detectTapGestures {
            onClickLocked?.invoke()
        }
    }

/**
 * Applies the appropriate modifier based on UiAccessState.
 * 
 * Usage:
 * ```
 * MyComponent(
 *     modifier = Modifier.applyAccessState(
 *         state = if (isFree) UiAccessState.DIM else UiAccessState.FULL,
 *         onLockedClick = { showPaywall() }
 *     )
 * )
 * ```
 */
fun Modifier.applyAccessState(
    state: UiAccessState,
    onLockedClick: (() -> Unit)? = null
): Modifier = when (state) {
    UiAccessState.FULL -> this
    UiAccessState.DIM -> this.dim()
    UiAccessState.LOCK -> this.lock(onClickLocked = onLockedClick)
}

// ============================================================
// LOCKED CONTAINER
// ============================================================

/**
 * A container that wraps content with a lock overlay.
 * Tapping anywhere in the container triggers the paywall.
 * 
 * Usage:
 * ```
 * if (isFree) {
 *     LockedContainer(onUnlockClick = { showPaywall() }) {
 *         TopDomainsList(domains)
 *     }
 * } else {
 *     TopDomainsList(domains)
 * }
 * ```
 */
@Composable
fun LockedContainer(
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .lock(onClickLocked = onUnlockClick)
    ) {
        content()
        
        // Lock icon overlay in top-right corner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Premium Feature",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
