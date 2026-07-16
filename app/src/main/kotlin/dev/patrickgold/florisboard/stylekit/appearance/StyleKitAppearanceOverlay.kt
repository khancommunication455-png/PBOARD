/*
 * Copyright (C) 2026 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.stylekit.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Optional overlay that renders the StyleKit appearance layers (background media,
 * dark scrim, glint sweep) on top of the keyboard background.
 *
 * To wire this into the live keyboard, add a single line in
 * `ime/text/TextInputLayout.kt` at the very top of the Composable's Box:
 *
 * ```
 * Box(modifier = Modifier.fillMaxSize()) {
 *     StyleKitAppearanceOverlay()   // <-- add this line
 *     // ... existing keyboard composables ...
 * }
 * ```
 *
 * The overlay is fully transparent when no background media and no glint are
 * configured, so it has zero visual impact by default.
 *
 * NOTE: this overlay only draws the *background* layers (media + scrim + glint).
 * The theme colors (key backgrounds, key text, accents) are intentionally NOT
 * overridden here, because doing so would conflict with FlorisBoard's Snygg
 * theming system. To make StyleKit themes fully take effect, you'd need to
 * generate a Snygg stylesheet from [StyleKitTheme] and register it with
 * [dev.patrickgold.florisboard.ime.theme.ThemeManager]. That's a documented
 * follow-up task — see the summary doc.
 *
 * Crash safety: every layer is independently wrapped; if the media URI fails
 * to load, only that layer is skipped.
 */
@Composable
fun StyleKitAppearanceOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { AppearanceRepository(context) }
    val config by repo.observe().collectAsState(initial = null)

    val cfg = config ?: return
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Background media (GIF or video) — only when configured.
        if (cfg.backgroundMediaUri.isNotBlank()) {
            MediaBackground(
                mediaUri = cfg.backgroundMediaUri,
                scrimAlpha = cfg.backgroundScrimAlpha,
            )
        }
        // 2. Glint sweep — only when enabled.
        if (cfg.glintEnabled) {
            GlintSweep(
                enabled = true,
                color = cfg.glintColor,
                speedMs = cfg.glintSpeedMs,
                opacity = cfg.glintOpacity,
            )
        }
    }
}
