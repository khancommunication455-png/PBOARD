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

package dev.patrickgold.florisboard.app.settings.typing

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

/**
 * StyleKit addition to the existing TypingScreen: surfaces the personalized
 * learning master toggle and the auto-swap toolbar/suggestion-row toggle,
 * both added in Part 1.
 *
 * The base TypingScreen content (Corrections, Suggestion group with incognito
 * mode, etc.) is unchanged — this Composable is meant to be embedded inside
 * the existing TypingScreen's `content { ... }` block to add the new prefs
 * without rewriting the whole screen.
 */
@Composable
fun StyleKitTypingExtras() {
    val prefs by FlorisPreferenceStore
    PreferenceGroup(title = "StyleKit — Adaptive Learning") {
        SwitchPreference(
            prefs.suggestion.personalizedLearning,
            title = "Personalized learning",
            summary = "Learn from your typing on this device to improve suggestions. " +
                "Nothing is ever sent off-device. Disable for full privacy or to match " +
                "incognito behavior globally.",
        )
        SwitchPreference(
            prefs.suggestion.autoSwapToolbarAndSuggestions,
            title = "Gboard-style toolbar swap",
            summary = "When the smartbar layout is set to 'Suggestions + Actions (Auto)', " +
                "the toolbar icons crossfade into 3 suggestion chips as soon as you start typing.",
        )
    }
}
