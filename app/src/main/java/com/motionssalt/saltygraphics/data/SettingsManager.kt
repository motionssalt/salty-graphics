package com.motionssalt.saltygraphics.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("salty_settings", Context.MODE_PRIVATE)

    private val _uiScale = MutableStateFlow(prefs.getFloat("ui_scale", 1.0f))
    val uiScale: StateFlow<Float> = _uiScale.asStateFlow()

    private val _useSystemDefault = MutableStateFlow(prefs.getBoolean("use_system_default", true))
    val useSystemDefault: StateFlow<Boolean> = _useSystemDefault.asStateFlow()

    fun setUiScale(scale: Float) {
        _uiScale.value = scale
        _useSystemDefault.value = false
        prefs.edit()
            .putFloat("ui_scale", scale)
            .putBoolean("use_system_default", false)
            .apply()
    }

    fun setUseSystemDefault(useDefault: Boolean) {
        _useSystemDefault.value = useDefault
        if (useDefault) {
            _uiScale.value = 1.0f
        }
        prefs.edit()
            .putBoolean("use_system_default", useDefault)
            .putFloat("ui_scale", 1.0f)
            .apply()
    }
}
