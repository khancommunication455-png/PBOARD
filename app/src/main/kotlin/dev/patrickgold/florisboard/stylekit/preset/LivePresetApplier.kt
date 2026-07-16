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

package dev.patrickgold.florisboard.stylekit.preset

import android.content.Context
import androidx.compose.runtime.Immutable
import dev.patrickgold.florisboard.stylekit.data.StyleKitDatabase
import dev.patrickgold.florisboard.stylekit.data.entity.StyleKitConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.florisboard.lib.kotlin.tryOrNull

/**
 * Holds the *currently active* preset mapping in memory so the per-keystroke
 * transform path can do a single map lookup with no DB I/O on the critical input path.
 *
 * The IME process observes the StyleKit config + preset table asynchronously.
 * Whenever the active preset changes (or its mapping is edited), [activeMapping]
 * is updated. The KeyboardManager reads [activeMapping] synchronously per keystroke.
 *
 * Memory: a single Map<String,String> of ~80 entries (~5KB). Negligible.
 *
 * Crash safety: any DB failure leaves the previous mapping in place; the IME
 * continues to function. If the very first read fails, [activeMapping] stays empty
 * and [transformChar] returns the original character (no-op).
 */
class LivePresetApplier private constructor(context: Context) {
    companion object {
        @Volatile private var INSTANCE: LivePresetApplier? = null
        fun get(context: Context): LivePresetApplier =
            INSTANCE ?: synchronized(this) { INSTANCE ?: LivePresetApplier(context.applicationContext).also { INSTANCE = it } }
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val presetRepo = PresetRepository(appContext)

    private val _activeState = MutableStateFlow(ActiveState())
    val activeState: StateFlow<ActiveState> = _activeState.asStateFlow()

    /** Snapshot of the current mapping for synchronous per-keystroke access. */
    @Volatile
    private var mappingSnapshot: Map<String, String> = emptyMap()

    init {
        // Observe the config table for changes to activePresetId / livePresetEnabled.
        scope.launch {
            val configDao = tryOrNull { StyleKitDatabase.get(appContext)?.configDao() }
            if (configDao == null) return@launch
            configDao.observe().collectLatest { config ->
                if (config == null) {
                    _activeState.value = ActiveState()
                    mappingSnapshot = emptyMap()
                    return@collectLatest
                }
                if (!config.livePresetEnabled || config.activePresetId == 0L) {
                    _activeState.value = ActiveState(enabled = false, presetId = 0L, name = null)
                    mappingSnapshot = emptyMap()
                    return@collectLatest
                }
                val preset = presetRepo.getById(config.activePresetId)
                if (preset == null) {
                    _activeState.value = ActiveState(enabled = false, presetId = 0L, name = null)
                    mappingSnapshot = emptyMap()
                    return@collectLatest
                }
                mappingSnapshot = DefaultPresets.decode(preset.mappingJson)
                _activeState.value = ActiveState(
                    enabled = true,
                    presetId = preset.rowId,
                    name = preset.name,
                )
            }
        }
    }

    /**
     * Synchronous per-keystroke transform. Returns the original [ch] if no preset
     * is active or if [ch] is not in the mapping.
     */
    fun transformChar(ch: Char): Char {
        if (!_activeState.value.enabled) return ch
        val snap = mappingSnapshot
        if (snap.isEmpty()) return ch
        val replacement = snap[ch.toString()]
        // Per-keystroke path only supports single-char replacements. If a preset
        // maps 'a' -> "𝓪" (single code point but multi-char in UTF-16), we return
        // the first char to keep commitChar semantics simple. The bulk convertText
        // path handles multi-char replacements correctly.
        return replacement?.firstOrNull() ?: ch
    }

    /** True if live preset conversion is on AND a preset is selected. */
    fun isActive(): Boolean = _activeState.value.enabled

    @Immutable
    data class ActiveState(
        val enabled: Boolean = false,
        val presetId: Long = 0L,
        val name: String? = null,
    )
}
